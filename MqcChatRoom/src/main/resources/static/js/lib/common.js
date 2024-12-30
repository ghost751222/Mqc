const hawkEyeUrlKey = "hawkEyeUrl";
const firstLineUrlKey= "firstLineUrl";
const serviceKey= "service";




function saveCookie(name,value)
{
    document.cookie = name + "=" + value;
}



function getCookie(name)
{
  return document.cookie
                        .split('; ')
                        .find(row => row.startsWith(name))
                        .split(name)[1].replace("=","");
}

 function deleteAllCookies() {

            var cookies = document.cookie.split(";");
            for (var i = 0; i < cookies.length; i++) {
                var cookie = cookies[i];
                var eqPos = cookie.indexOf("=");
                var name = eqPos > -1 ? cookie.substr(0, eqPos) : cookie;
                document.cookie = name + "=;expires=Thu, 01 Jan 1970 00:00:00 GMT";
            }
}

 function returnLogin(){
          deleteAllCookies()
          localStorage.removeItem('isLoaded');
          location.href = "/login"
 }



function webSocketConnect(){
    var url  =  location.origin + "/chat";
    var username = localStorage.getItem("username")
    var socket = new SockJS(url, undefined, {protocols_whitelist: ['websocket']});
    var stompClient = Stomp.over(socket);
        stompClient.connect({}, function(frame) {
	     stompClient.subscribe("/topic/logout", function(message){
       		   var jsonObject = JSON.parse(message.body);
       		   if(jsonObject.username == username)
       		      returnLogin()
       	    });
});

}