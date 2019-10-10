# APACHE CAMEL Example

# Expose REST by Consuming  SOAP

REST Req -> cxf SOAP Client Call -> REST Resp

http://localhost:8080/say/add

{
  "intA": 1,
  "intB": 2
}


Domain SOAP Service src/main/resources/service.wsdl


# Expose REST by Consuming  REST

REST Req -> HTTP Client Call -> REST Resp

http://localhost:8080/say/emp/1

Domain REST Service http://localhost:8082/employees


# Camel K Deployment Steps

