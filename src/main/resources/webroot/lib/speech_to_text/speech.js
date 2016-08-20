for (var i = 0; i < langs.length; i++) {
  select_language.options[i] = new Option(langs[i][0], i);
}
select_language.selectedIndex = 6;
updateCountry();
select_dialect.selectedIndex = 6;
showInfo('info_start');

function updateCountry() {
  for (var i = select_dialect.options.length - 1; i >= 0; i--) {
    select_dialect.remove(i);
  }
  var list = langs[select_language.selectedIndex];
  for (var i = 1; i < list.length; i++) {
    select_dialect.options.add(new Option(list[i][1], list[i][0]));
  }
  select_dialect.style.visibility = list[1].length == 1 ? 'hidden' : 'visible';
}

var api_function = false;
var final_transcript = '';
var recognizing = false;
var ignore_onend;
var start_timestamp;

var sentimentText  = ["Very Negative","Negative", "Neutral", "Positive", "Very Positive"];
var sentimentColor = ["red-600", "orange-800", "blue-grey-500", "teal-400", "green-600"];

//var eb = new EventBus('/eventbus/');

/*eb.onopen = function () {
  //eb.publish('events', {"message":"hello","from":"js"});
  eb.registerHandler('events', function (err, msg) {
    if (err){
      console.error(err)
    }
    console.log(msg);
    //$('#log').prepend('<div>' + msg.body.message + '-' + msg.body.from + '</div>');
  });
  //error_showSnackbar();
  //eb.send('events', {"message":"hello","from":"js"});
};*/

function error_showSnackbar() {
  var snackbarContainer = document.querySelector('.mdl-js-snackbar');
  snackbarContainer.MaterialSnackbar.showSnackbar({message: 'An unknown error has occurred.'});
}

var SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
if (!SpeechRecognition) {
  upgrade();
} else {
  start_button.style.display = 'inline-block';
  var recognition = new SpeechRecognition();
  recognition.continuous = true;
  recognition.interimResults = true;

  recognition.onstart = function() {
    recognizing = true;
    showInfo('info_speak_now');
    start_img.src = 'img/mic-animate.gif';
  };

  recognition.onerror = function(event) {
    if (event.error == 'no-speech') {
      start_img.src = 'img/mic.gif';
      showInfo('info_no_speech');
      ignore_onend = true;
    }
    if (event.error == 'audio-capture') {
      start_img.src = 'img/mic.gif';
      showInfo('info_no_microphone');
      ignore_onend = true;
    }
    if (event.error == 'not-allowed') {
      if (event.timeStamp - start_timestamp < 100) {
        showInfo('info_blocked');
      } else {
        showInfo('info_denied');
      }
      ignore_onend = true;
    }
  };

  recognition.onend = function() {
    recognizing = false;
    if (ignore_onend) {
      return;
    }
    start_img.src = 'img/mic.gif';
    if (!final_transcript) {
      showInfo('info_start');
      return;
    }
    showInfo('');
    if (window.getSelection) {
      window.getSelection().removeAllRanges();
      var range = document.createRange();
      range.selectNode(document.getElementById('final_span'));
      window.getSelection().addRange(range);
    }
    if (api_function) {
      api_function = false;
      sendAPI();
    }
  };

  recognition.onresult = function(event) {
    var interim_transcript = '';
    for (var i = event.resultIndex; i < event.results.length; ++i) {
      if (event.results[i].isFinal) {
        final_transcript += event.results[i][0].transcript;
        sendAPI(); //for the automatic recognition
      } else {
        interim_transcript += event.results[i][0].transcript;
      }
    }
    final_transcript = capitalize(final_transcript);
    //final_span.innerHTML = linebreak(final_transcript);
    final_span.innerHTML += linebreak(final_transcript); //for the automatic recognition
    interim_span.innerHTML = linebreak(interim_transcript);
    if (final_transcript || interim_transcript) {
      showButtons('inline-block');
    }
  };
}

function upgrade() {
  start_button.style.visibility = 'hidden';
  showInfo('info_upgrade');
}

var two_line = /\n\n/g;
var one_line = /\n/g;
function linebreak(s) {
  return s.replace(two_line, '<p></p>').replace(one_line, '<br>');
}

var first_char = /\S/;
function capitalize(s) {
  return s.replace(first_char, function(m) { return m.toUpperCase(); });
}

function sendAPI() {
  var n = final_transcript.indexOf('\n');
  if (n < 0 || n >= 80) {
    n = 40 + final_transcript.substring(40).indexOf(' ');
  }
  var textURI = encodeURI(final_transcript);
  //var JSONtext = {"message":final_transcript,"activity":final_activity,"from":"js"}
  var JSONtext = JSON.stringify({sentence : final_transcript,sentiment:"",subject:"",verb:"",object:"",userId : myUserId});

  //eb.publish('events', {"message":final_transcript,"from":"js"});
  $.post("/api/feedbacks", JSONtext, function(data) {
    console.log("[TRYING] - Send the message");
    console.log(JSONtext);
    })
    .done(function(data) {
      console.log("[SUCCESS] - Message sent");
      console.log(data);
      for(var i=0;i<data.length;i++){
        if(data[i].subject==="") { } else {
          $('#log').prepend('<span>' + '(' + data[i].subject + ' ,' + data[i].verb + ', ' + data[i].object + ')'+'</span>');
        }
        $('#log').prepend('<span>' + data[i].sentence + ' [' + data[i].sentiment + ' on a scale of 0→4]' + '</span><br>');
        $('#log').prepend('<h3 class="mdl-card__title-text mdl-color-text--'+sentimentColor[data[i].sentiment]+'">' + 'Your sentiment is ' + sentimentText[data[i].sentiment] + '</h3>');

      }
    })
    .fail(function(data) {
      console.error('[ERROR] - to send the message:' + data);
      error_showSnackbar();
    })
    .always(function(data) {
      console.log("Request finished");
      final_transcript = ''; //for the automatic recognition
  }, "json");
  /*eb.send('events', JSONtext, function(err, reply) {
        if (err) {
          console.error('[ERROR] - to send the message:' + err);
          error_showSnackbar();
        } else {
          console.log('[SUCCESS] - Message sent');
          console.log(JSONtext);
        }
        if (reply) {
          console.log(reply);
          if(reply.hasOwnProperty('body')) {
            if(reply.body.hasOwnProperty('sentimentArray')) {
              for(var i=0;i<reply.body.sentimentArray.length;i++){
                if(reply.body.subjectArray[i].subject==="") { } else {
                  $('#log').prepend('<span>' + '(' + reply.body.subjectArray[i].subject + ' ,' + reply.body.subjectArray[i].relation + ', ' + reply.body.subjectArray[i].object + ')'+'</span>');
                }
                $('#log').prepend('<span>' + reply.body.sentimentArray[i].sentence + ' [' + reply.body.sentimentArray[i].sentiment + ' on a scale of 0→4]' + '</span><br>');
                $('#log').prepend('<h3 class="mdl-card__title-text mdl-color-text--'+sentimentColor[reply.body.sentimentArray[i].sentiment]+'">' + 'Your sentiment is ' + sentimentText[reply.body.sentimentArray[i].sentiment] + '</h3>');
                final_transcript = ''; //for the automatic recognition
              }
            }
          } else {
            console.error('[ERROR] - no body on the reply message: ' + err);
            error_showSnackbar();
          }
        } else {
          console.error('[ERROR] - reply timeout: ' + err);
          error_showSnackbar();
        }
      }
    );*/
}

function copyButton() {
  if (recognizing) {
    recognizing = false;
    recognition.stop();
  }
  copy_button.style.display = 'none';
  copy_info.style.display = 'inline-block';
  showInfo('');
}

function apiButton() {
  if (recognizing) {
    api_function = true;
    recognizing = false;
    recognition.stop();
  } else {
    final_transcript = final_span.innerHTML; //for the automatic recognition
    sendAPI();
  }
  api_button.style.display = 'none';
  api_info.style.display = 'inline-block';
  showInfo('');
}

function startButton(event) {
  if (recognizing) {
    recognition.stop();
    return;
  }
  final_transcript = '';
  recognition.lang = select_dialect.value;
  recognition.start();
  ignore_onend = false;
  final_span.innerHTML = '';
  interim_span.innerHTML = '';
  start_img.src = 'img/mic-slash.gif';
  showInfo('info_allow');
  showButtons('none');
  start_timestamp = event.timeStamp;
}

function showInfo(s) {
  if (s) {
    for (var child = info.firstChild; child; child = child.nextSibling) {
      if (child.style) {
        child.style.display = child.id == s ? 'inline' : 'none';
      }
    }
    info.style.visibility = 'visible';
  } else {
    info.style.visibility = 'hidden';
  }
}

var current_style;
function showButtons(style) {
  if (style == current_style) {
    return;
  }
  current_style = style;
  copy_button.style.display = style;
  api_button.style.display = style;
  copy_info.style.display = 'none';
  api_info.style.display = 'none';
}

////////////////////////////////////
activity_span.innerHTML = "Restauration";
final_activity = activity_span.innerHTML;

$('ul.mdl-menu li').click(function(e)
{
  activity_span.innerHTML = $(this).html();
  final_activity = activity_span.innerHTML;
  //console.log($(this).html());
 });
