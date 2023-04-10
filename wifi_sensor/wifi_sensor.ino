#include "SoftwareSerial.h"
#include <dht.h>

//static struct pt thread1; //建立兩個任務

#define dht_dpin 2
#define DEBUG true
dht DHT;
int air_alg = A0;
int piezoPin=9;
int sensorvalue;


String sendData(String command, const int timeout, boolean debug){
  String response = "";
  Serial1.println(command);
  long int time = millis();
  while( (time+timeout) > millis()){
    while(Serial1.available()){

      char c = Serial1.read();
      response+=c;
    }
  }
  if(debug){
    Serial.println(response);
  }
  return response;
 }


void setup() {
  // put your setup code here, to run once:
 delay(10);
Serial1.begin(115200);
Serial.begin(115200);
pinMode(piezoPin, OUTPUT);
sendData ("AT+RST\r\n",2000,DEBUG);//重新起始 ESP8266
sendData ("AT+CWMODE=3\r\n",200,DEBUG);//查詢目前工作模式,模式 2 (AP 基地台模式), 1=STA 網卡模式, 3=BOTH (AP+STA).
if(sendData("AT+CWJAP=\"12s\",\"12345678\"\r\n",5000,DEBUG)){
// 連線指定之基地台 (Join AP),AT+CWJAP="EDIMAX-tony","123456789111"       
  Serial1.print("Join AP succeess\r\n");
  Serial1.print("IP:");
  sendData("AT+CIFSR\r\n",5000,DEBUG);//查詢 IP 位址
  sendData("AT+CIPSER=0\r\n",200,DEBUG);  
  //設定是否開啟 ESP8266 為伺服器用 CIPMUX=1 開啟多重連線後
  //就可以用 CIPSERVER 設定 ESP8266 的伺服器功能. mode=1 為開啟伺服器, 必須指定 port; mode=0 為關閉伺服器
  // 不須指定 port. 在單一連線下開啟伺服器, 會得到 ERROR 回應.
  sendData("AT+CIPMUX=0\r\n",200,DEBUG);
  //詢問目前 TCP/UDP 連線模式
  //這是設定 ESP8266 與同一 WiFi 網路中其他裝置的連線模式, 預設為單一連線,
  //如果要讓 ESP8266 當伺服器用, 必須設為 1=多重連線. 
}
 
}

void loop() {
  // put your main code here, to run repeatedly:
//  thread1_entry(&;thread1);
  
  DHT.read11(dht_dpin);
    sendData("AT+CIPSTART=\"TCP\",\"192.168.43.112\",80\r\n",1000,DEBUG);
    float temp = DHT.temperature;
    
    String aa = "GET /test_get.php?name=temp&value=";  
    aa += temp;
    aa +=" HTTP/1.1\r\nHost: 192.168.43.112\r\n\r\n";
    String cipSend = "AT+CIPSEND=";
    cipSend +=aa.length();
    cipSend +="\r\n";
    sendData(cipSend,300,DEBUG);
    sendData(aa,300,DEBUG);
//    cipSend = "AT+CIPSEND=";
//    if(sensorvalue>100){
//       tone(piezoPin,1000,2000);
//    }
 //   delay(200);

    float humidity = DHT.humidity;  
    
    String bb = "GET /test_get.php?name=humidity&value=";
    bb += humidity;
    bb +=" HTTP/1.1\r\nHost: 192.168.43.112\r\n\r\n";
    cipSend = "AT+CIPSEND=";
    cipSend +=bb.length();
    cipSend +="\r\n";
    sendData(cipSend,300,DEBUG);
    sendData(bb,300,DEBUG);
//    if(sensorvalue>100){
//      tone(piezoPin,1000,2000);
//    }
//delay(200);
//////////////////////////////////////////////////
    sensorvalue=analogRead(air_alg);
    
    String cc = "GET /test_get.php?name=airtype&value=";
    cc +=sensorvalue;
    cc +=" HTTP/1.1\r\nHost: 192.168.43.112\r\n\r\n";
    cipSend = "AT+CIPSEND=";
    cipSend +=cc.length();
    cipSend +="\r\n";
    sendData(cipSend,300,DEBUG);
    sendData(cc,300,DEBUG);
   // sendData("AT+CIPCLOSE\r\n",500,DEBUG);
//    if(sensorvalue>100){     
//      tone(piezoPin,1000,2000);
//    }
//delay(200);
////////////////////////////////////////////////////////
    
    //將光敏電阻數值轉換成百分比    
    int pr = analogRead(A8);
    int light = (float)(pr+1)/128*100;
    
    String dd = "GET /test_get.php?name=light&value=";
    dd +=light;
    Serial.print("light="+light);
    dd +=" HTTP/1.1\r\nHost: 192.168.43.112\r\n\r\n";
    cipSend = "AT+CIPSEND=";
    cipSend +=dd.length();
    cipSend +="\r\n";
    sendData(cipSend,300,DEBUG);
    sendData(dd,300,DEBUG);
    sendData("AT+CIPCLOSE\r\n",300,DEBUG);
//    if(sensorvalue>100){     
//      tone(piezoPin,1000,2000);
//    }
    
}
