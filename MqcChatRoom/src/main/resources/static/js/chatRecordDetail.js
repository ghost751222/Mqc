Vue.prototype.$http = axios;


const vue = new Vue({
	el : '#app',
	data : {
		url:'ws',
		ws:null,
        extension:"",
        caller:"",
        isAdmin:false,
		chatRoom : document.querySelector("#chatRoom"),
		chatData : [],
		isSpeakVoice:false,
	},methods:{
		connectToWs(){

            var _this = this
			var socket = new SockJS(this.url);
			this.ws = Stomp.over(socket);
			var user= {'name':this.extension};
			const ws = this.ws;
			ws.connect(user,function(frame){
                _this.IsConnect =true
				ws.subscribe("/topic/getResponse", function(message) {

				        let d = JSON.parse(message.body);
				        if(d.caller == _this.caller || d.callee == _this.caller){
				           _this.chatData.push(d)
				           _this.$nextTick(()=>{
				              chatRoom.scrollTop = chatRoom.scrollHeight;
                              document.body.scrollTop = document.body.scrollHeight;
				           })

				        }
				});

			},function(error) {
				console.error("WebScoker Connect error " + error);
				_this.IsConnect =false;
			})
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
	},

	mounted:function(){
	    var _this = this;
        var params = new URLSearchParams(location.search);
        this.requestPath = chatRecordDetail +"/chatRecord" + location.search
        this.caller = params.get("extension");
        if(params.get("id")){
            axios.get(this.requestPath).then(data=>{
                        _this.chatData = data.data
            }).catch(err=>{
                       alert(err)
            })
        }else{
          this.connectToWs()
        }

	}
})