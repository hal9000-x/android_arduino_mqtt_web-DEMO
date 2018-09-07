#include <ESP8266WiFi.h>
#include <PubSubClient.h>
#include <WiFiClient.h>
#include <ESP8266WebServer.h>


const char* ssid = "Your-WIFI-SSID";
const char* password = "YOUR-WIFI-PASSWORD";

const char* mqtt_id = "led/1";
const char* mqtt_server = "192.168.1.80"; //MQTT server IP

ESP8266WebServer server(80); //I have used a different port and used port forwarding from router to access it from Internet


int led_pin = 5; //GPIO 5
int buttonPin = 4; // GPIO 4                                                                                                                                                                                                                                                                                             ;

WiFiClient espClient;
PubSubClient client(espClient);
long lastMsg = 0;
char msg[50];
int value = 0; 

void handleLEDon() { 
 Serial.println("LED on page");
 digitalWrite(led_pin,HIGH);
 client.publish(mqtt_id, "confirmed_on");
 server.send(200, "text/html", "LED is ON");
}

void handleLEDoff() { 
 Serial.println("LED off page");
 digitalWrite(led_pin,LOW);
 client.publish(mqtt_id, "confirmed_off");
 server.send(200, "text/html", "LED is OFF");
}

void setup_wifi() {

  delay(10);
  Serial.println();
  Serial.print("Connecting to ");
  Serial.println(ssid);

  WiFi.begin(ssid, password);

  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }

  randomSeed(micros());

  Serial.println("");
  Serial.println("WiFi connected");
  Serial.println("IP address: ");
  Serial.println(WiFi.localIP());
}

void callback(char* topic, byte* payload, unsigned int length) {
  Serial.print("Message arrived [");
  Serial.print(topic);
  Serial.print("] ");
  String s;
  for (int i = 0; i < length; i++) {
    s+=(char)payload[i];
  }
  Serial.print(s);
  Serial.println();

  //converting String to char array
  int str_len = s.length() + 1;
  char char_array[str_len];
  s.toCharArray(char_array, str_len);
  client.publish("outTopic", char_array);

  //TEST CONNECTION
  if(s.indexOf("test") != -1)  {
    if(digitalRead(led_pin)){
      client.publish(mqtt_id, "connected-on");
    }
    else
    {
      client.publish(mqtt_id, "connected-off");
    }
  }

  // TURN ON THE LED
  if(s.indexOf("led_on") != -1)  {
    digitalWrite(led_pin, HIGH);
    client.publish(mqtt_id, "confirmed_on");
  }

  // TURN OFF THE LED
  if(s.indexOf("led_off") != -1)  {
    digitalWrite(led_pin, LOW);
    client.publish(mqtt_id, "confirmed_off");
  }

}

void reconnect() {
  while (!client.connected()) {
    Serial.print("Attempting MQTT connection...");
    String clientId = "ESP8266Client-";
    clientId += String(random(0xffff), HEX);
    
    if (client.connect(clientId.c_str(), "MQTT_USER", "MQTT_PASSWORD", mqtt_id, 2, true, "connection_lost")) {
      Serial.println("connected");
      client.publish(mqtt_id, "connected");
      client.subscribe(mqtt_id);
    } else {
      Serial.print("failed, rc=");
      Serial.print(client.state());
      Serial.println(" try again in 5 seconds");
      delay(5000);
    }
  }
}

void setup() {
  Serial.begin(9600);
  delay(10);

  pinMode(led_pin, OUTPUT);
  pinMode(buttonPin, INPUT_PULLUP);
  setup_wifi();
  client.setServer(mqtt_server, 1883);
  client.setCallback(callback);

  server.on("/ON", handleLEDon);
  server.on("/OFF", handleLEDoff);

  server.begin();
  Serial.println("HTTP server started");
}

void loop() {

  if (!client.connected()) {
    reconnect();
  }
  client.loop();
  server.handleClient();

  // Handling LED toggle button, use interrupt instead
  if (digitalRead(buttonPin) == LOW) {
    if(digitalRead(led_pin)){
      digitalWrite(led_pin, LOW);
      client.publish(mqtt_id, "connected-off");
    }
    else
    {
      digitalWrite(led_pin, HIGH);
      client.publish(mqtt_id, "connected-on");
    }
    delay(500);
  }
  
}
