package com.javainuse.route;

import com.cdyne.ws.weatherws.GetCityWeatherByZIPResponse;
import com.cdyne.ws.weatherws.WeatherReturn;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.javainuse.EmployeeDTO;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.rest.RestBindingMode;
import org.apache.cxf.message.MessageContentsList;
import org.tempuri.Add;
import org.tempuri.AddResponse;

import javax.ws.rs.core.MediaType;


public class SimpleRouteBuilder extends RouteBuilder {


    @Override
    public void configure() {

        // Camel Rest Initialization and Doc
        restConfiguration()
                .component("netty4-http").bindingMode(RestBindingMode.json)
                .host("localhost").contextPath("/").port("8080")
                .apiContextPath("/api-doc")
                .apiProperty("api.title", "User API").apiProperty("api.version", "1.2.3")
                // and enable CORS
                .apiProperty("cors", "true")
                .bindingMode(RestBindingMode.auto);

        getContext().setTracing(true);

        // Expose Rest Routes
        rest("/say")
                .produces(MediaType.APPLICATION_JSON)
                .consumes(MediaType.APPLICATION_JSON)
                .get("/hello").to("direct:hello")
                .get("/bye").consumes("application/json").to("direct:bye")
                .post("/testmockupdate").to("mock:update")
                .post("/add").type(Add.class).outType(AddResponse.class).to("direct:add")
                .get("/weather/{zip}").outType(WeatherReturn.class).to("direct:getCityWeatherByZIP")
                .get("/emp/{id}").outType(EmployeeDTO.class).to("direct:getEmployee");

        //.get("/doc").outType(DocumentPackageGroups.class).to("direct:getCityWeatherByZIP");

        // Sample Routes
        from("direct:hello")
                .log("hello called")
                .process(exchange -> exchange.getIn().setBody("{'hello':'tewt'}"))
                .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(201));

        from("direct:bye")
                .transform().constant("Bye World");

        // Rest Req -> SOAP client Call -> Rest Resp

        from("direct:add")
                .log(LoggingLevel.INFO, " Body : ${body}")
                .process(exchange -> {
                    Add a = exchange.getIn().getBody(Add.class);
                    exchange.getOut().setBody(new Object[]{new Integer(a.getIntA()), new Integer(a.getIntB())});

                })
                .setHeader("operationName", constant("Add"))
                .to("cxf://http://www.dneonline.com/calculator.asmx?serviceClass=org.tempuri.CalculatorSoap&wsdlURL=src/main/resources/service.wsdl&loggingFeatureEnabled=true")
                .log(LoggingLevel.INFO, "The response was ${body}").setHeader(Exchange.CONTENT_TYPE, constant("application/json")).process(
                exchange -> {
                    MessageContentsList soapMessage = (MessageContentsList) exchange.getIn().getBody();
                    if (soapMessage == null) {
                        System.out.println("Incoming null message detected...");

                    }

                    Integer test = (Integer) soapMessage.get(0);

                    System.out.println(test);

                    AddResponse ar = new AddResponse();
                    ar.setAddResult(test);

                    exchange.getOut().setBody(ar);

                });

        /**from("direct:getCityWeatherByZIP")
                .process(exchange -> {
                    String zip = exchange.getIn().getHeader("zip", String.class);
                    System.out.println(zip);
                    exchange.getOut().setBody(zip);
                })
                .setHeader("operationName", constant("GetCityWeatherByZIP"))
                .to("cxf://http://wsf.cdyne.com/WeatherWS/Weather.asmx?serviceClass=com.cdyne.ws.weatherws.WeatherSoap&wsdlURL=src/main/resources/weather.wsdl&loggingFeatureEnabled=true")
                .log(LoggingLevel.INFO, "The response was ${body}").process(
                exchange -> {
                    GetCityWeatherByZIPResponse w = exchange.getIn().getBody(GetCityWeatherByZIPResponse.class);
                    if (w == null) {
                        System.out.println("Incoming null message detected...");

                    }
                    exchange.getOut().setBody(w);
                });*/

        // Rest Req -> Rest client Call -> Rest Resp

        // route for REST GET Call
        from("direct:getEmployee")
                .log(LoggingLevel.INFO, " Header : ${header.id}")
                .removeHeader(Exchange.HTTP_URI)
                .removeHeader(Exchange.HTTP_PATH)
                .setHeader(Exchange.HTTP_METHOD, simple("GET"))
                .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
                .setHeader(Exchange.HTTP_PATH, simple("${header.id}"))
                .streamCaching()
                .to("http://localhost:8082/employees")
                .log(LoggingLevel.INFO, "${body}")
                .process(
                    exchange -> {

                        String jsonGotFromCall = exchange.getIn().getBody(String.class);

                        //Unmarshall the JSON to Pojo
                        ObjectMapper mapper = new ObjectMapper();
                        EmployeeDTO emp = mapper.readValue(jsonGotFromCall, EmployeeDTO.class);
                        emp.setOrg("CS");
                        // Do Any Convertion
                        exchange.getOut().setBody(emp);
                 });

        /** route for REST POST Call
         from("file:C:/inboxPOST?noop=true").process(new CreateEmployeeProcessor()).marshal(jsonDataFormat)
         .setHeader(Exchange.HTTP_METHOD, simple("POST"))
         .setHeader(Exchange.CONTENT_TYPE, constant("application/json")).to("http://localhost:8080/employee")
         .process(new MyProcessor());*/


    }

}