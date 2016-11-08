#docker build -f Dockerfile -t davidaparicio/myapp:latest .
mvn clean package -DskipTests
docker-compose build app
docker-compose up app
