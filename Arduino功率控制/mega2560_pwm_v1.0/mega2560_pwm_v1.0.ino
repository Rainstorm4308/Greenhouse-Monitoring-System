#include <SoftwareSerial.h>
#define DEBUG true
#include <TimerOne.h>

const byte pin = 4;             // TRIAC訊號輸出接腳
const byte trigPin = 2;      // TRIAC使用   只能用pin2或3
volatile boolean zeroCross = false;   //目前trig狀態 // 儲存零交越狀態的變數
volatile float dim = 64;  
volatile int i = 0;
volatile float dim_tmp = 0;
int val;

void setup() {
  pinMode(pin, OUTPUT);
  pinMode(trigPin, INPUT_PULLUP);
  attachInterrupt(digitalPinToInterrupt(trigPin), zeroCrossISR, RISING);
  //attachInterrupt(0 , zeroCrossISR , RISING); //注意, 0代表的是中斷編號,不是腳位哦！D2腳位的中斷編號為0, 所以此處需放0 
  
  Timer1.initialize(65);    // 設定讓定時器每隔65微秒，自動執行dim_check函數。
  Timer1.attachInterrupt(dim_check);
  
  Serial.begin(115200);
  }

void zeroCrossISR() {
  zeroCross = true;      
  i=0;
  digitalWrite(pin, LOW);  // 關閉燈泡TRIAC
  dim = dim_tmp;
}

void dim_check() {
  if (zeroCross) {                         // 若已經過零交越點....
    if (i > 127-dim) {                       // 判斷是否過了延遲觸發時間...
      digitalWrite(pin, HIGH);              // 開啟TRIAC
      i = 0;                             // 重設「計數器」
      zeroCross = false;                   //到下個零交越
    }else {
      i++;   // 增加「計數器」
    }
  }
}

void loop() {
  if (Serial.available()) { // check if esp8266 is sending message
    if (Serial.find("pwm=")) {
      val=Serial.parseInt();
      Serial.println("pwm is " + val);
    }
    
    dim_tmp = val;
    dim_tmp = (dim_tmp/100)*128-1;
   }
 }
