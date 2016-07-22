docker ps -a | grep 'weeks ago' | awk '{print $1}' | xargs --no-run-if-empty docker rm

#docker ps -a | docker rm
#docker images| docker rmi

#REPOSITORY            TAG                 IMAGE ID            CREATED             SIZE
#davidaparicio/myapp   latest              0b582c05341f        35 seconds ago      131.1 MB
#anapsix/alpine-java   jre8                2f5c81ce3b29        2 days ago          122.3 MB
#mongo                 3.2.8               7f09d45df511        7 days ago          336.1 MB
