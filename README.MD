# Transfer-Encoding Header POC

This demonstrates how Spring Boot might inadvertently generate _Transfer-Encoding_ headers that will break Nginx/Golang 1.15+

It also shows how you can inject a Tomcat valve to log/capture the headers before sending the response.

