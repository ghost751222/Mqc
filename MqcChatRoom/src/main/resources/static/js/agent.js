var app = new Vue({
    "el": "#app",
    data: {
        title: "Page",
        wsUrl: 'ws',
        baseUrl: null,
        url: null,
        dataPath: "",
        agentData: [],
        timeoutID:null,
        showModal: false,

    },
    methods: {
      callback(data){

         this.toggleClass(data.caller)
         this.toggleClass(data.callee)

        setTimeout(()=>{
                  this.toggleClass(data.caller)
                          this.toggleClass(data.callee)
        },1500)

      },
      toggleClass(extension){
        var selector = `[extension="${extension}"]`
        var d = document.querySelector(selector)
        if(d) {
           d.classList.toggle("border-warning")
           d.classList.toggle("text-danger")
        }
      },
      openDialog(e){

           this.showModal = true
                  var _this = this
                  var src = chatRecordDetailPath  + "?extension=" + e.currentTarget.getAttribute("extension");

                    this.$nextTick (()=>{
                        _this.$refs.iframe.src= src
                    })
      }
    },
    mounted: function () {
            this.connectToWS(this.callback);

            this.baseUrl = userInfoPath;
            this.url = this.baseUrl + "/findAllByIsAdmin"
            var _this = this;

            axios.get(this.url).then(res => {



                  var d = {}
                  var quotient =0
                  var cell = 5
                  for( i in res.data){
                     if(!this.isAdmin && res.data[i].extension != this.caller) continue;
                     quotient = parseInt(i/cell,10)
                     if(!d[quotient]) d[quotient]=[]
                     d[quotient].push(res.data[i])
                  }

                  if(d[quotient] && d[quotient].length <5){
                       var q= (cell -d[quotient].length);
                       for(let i = 0; i < q ; i++){
                         d[quotient].push({});
                       }
                  }


                  this.agentData = d



            }).catch((error) => {

                alert(error)
            });




        }
    })