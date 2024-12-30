Vue.prototype.$http = axios;


const vue = new Vue({
	el : '#app',
	data : {
		message:null,
		url:'ws',
		ws:null,
		requestPath : "",
		caller:"",
		callee:"",
        to_lang:"en",
        lang_data:[{value:"en",text:"英文"},{value:"zh-TW",text:"繁體中文"}],
        displayValue :"Auto Scroll",
        IsConnect :null,
		chatRoom : document.querySelector("#chatRoom"),
		chatData : [],
		isSpeakVoice:false
	},methods:{
		clearChatData(){
			vue.chatData=[];
		},autoScroll(){
			if(vue.IsAutoScroll){
				vue.IsAutoScroll = false
				vue.displayValue ="Manual Scroll"
			}else{
				vue.IsAutoScroll = true
				vue.displayValue ="Auto Scroll"
			}
		},connectToWs(){

			var socket = new SockJS(vue.url);
			vue.ws = Stomp.over(socket);
			var user= {'name':vue.caller};
			const ws = this.ws;
			ws.connect(user,function(frame){
				vue.IsConnect =true;


				ws.subscribe('/topic/getResponse',function(greeting){

                    vue.chatData.push(greeting.body);
			    });

				ws.subscribe('/user/queue/' + vue.caller,function(data){

			        let d = JSON.parse(data.body);
			        //d.caller = vue.callee;
			        vue.chatData.push(d);
                    console.log(vue.chatData)
			    });

			    ws.subscribe('/user/queue/' + vue.callee,function(data){
			      console.log(data.body);
                    let d = JSON.parse(data.body);
                	vue.chatData.push(d);
                });
//				ws.send("/app/welcome", {}, JSON.stringify({ 'name': 'welcome' }));
//				ws.send("/app/message", {}, JSON.stringify({ 'name': 'messaage' }));

				//ws.send("/app/send", {}, JSON.stringify({ 'name': '1234555' }));
			},function(error) {
				console.error("WebScoker Connect error " + error);
				vue.IsConnect =false;
			})
		},sendMessage(){

			var requestMessage = {'caller':vue.caller ,'callee': vue.callee,'message':this.message,'to_lang':this.to_lang };

			this.ws.send("/app/send", {}, JSON.stringify(requestMessage));
			//vue.chatData.push(requestMessage);
		},disconnectToWs(){
			if(this.ws !=null){
				this.ws.disconnect();
				this.IsConnect =false
			}
		},play(message){

		  if(!this.isSpeakVoice){
                           this.isSpeakVoice = true
                 		  const utterance = new SpeechSynthesisUtterance(message);

                             // Select a voice
                             const voices = speechSynthesis.getVoices();
                             utterance.voice = voices[0]; // Choose a specific voice

                             // Speak the text
                             speechSynthesis.cancel()
                             speechSynthesis.speak(utterance);
                             this.isSpeakVoice = false
                        }
		}
	},filters:{
		moment: function (timestamp) {
		    return moment(timestamp).format('YYYY/MM/DD HH:mm:ss');
		  }
	},
	updated:function(){
		if(vue.IsAutoScroll){
		  chatRoom.scrollTop = chatRoom.scrollHeight;
		  document.body.scrollTop = document.body.scrollHeight;
		}

	},
	mounted:function(){
	    var _this = this
        document.addEventListener("keypress", function(event) {
                         if (event.key === "Enter") {
                           event.preventDefault();
                            _this.sendMessage();
                         }
                       });
	}
})