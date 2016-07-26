docker ps -a | grep 'weeks ago' | awk '{print $1}' | xargs --no-run-if-empty docker rm

#docker volume ls | awk '{print $2}' | xargs docker volume rm
#ps -A | grep -Ei "[v]oivi.*configIT" | awk '{print $1}' | xargs kill -SIGTERM

#docker ps -a | docker rm
#docker images| docker rmi

#REPOSITORY            TAG                 IMAGE ID            CREATED             SIZE
#voivi_app             latest              8f666ec75b1a        20 hours ago        130.4 MB
#anapsix/alpine-java   jre8                2f5c81ce3b29        7 days ago          122.3 MB
#mongo                 3.2.8               7f09d45df511        12 days ago         336.1 MB
#jenkins               2.7.1-alpine        1b5319406d90        12 days ago         266.3 MB
#sonarqube             5.6-alpine          d3666ea9bde0        2 weeks ago         290.3 MB
