docker run --name voivi_database -p 27017:27017 -d mongo:3.2.8
docker run --name voivi_app -p 10101:10101 davidaparicio/myapp
