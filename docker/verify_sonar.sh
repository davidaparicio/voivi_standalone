mvn cobertura:cobertura -Dcobertura.report.format=xml
#mvn sonar:sonar
mvn sonar:sonar -Dsonar.host.url="http://192.168.99.100:9000" -Dsonar.jdbc.url="h2://192.168.99.100/sonar" -Dsonar.cobertura.reportPath="target/site/cobertura/coverage.xml"
