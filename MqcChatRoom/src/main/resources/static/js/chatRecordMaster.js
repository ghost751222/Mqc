  var app = new Vue({
            "el":"#app",
             data:{
                title:"Page",
                perPage :10,
                baseUrl:null,
                url:null,
                dataPath :"",
                paginationPath :"pagination",
                appendParams:{},

                startDateTime:"",
                endDateTime:"",
                agentExtension:"",
                customerNumber:"",

                showModal: false,
                sortOrder : [{field:"createTime",direction:"desc"}],
                fields: [
                    { "name": "callId", "title": "CallID"},
                    { "name": "agentAccount", "title": "AgentAccount"},
                    { "name": "agentName", "title": "AgentName"},
                    { "name": "agentExtension", "title": "AgentExtension"},
                    { "name": "direction", "title": "Direction"},
                    { "name": "customerNumber", "title": "CustomerNumber"},
                    { "name": "createTime", "title": "createTime",sortField:"createTime"},
                    { "name": "actions", "title": "Actions" },

                ],
             },
             methods:{
               query:function(e){
                    this.appendParams= {}

                    if(!this.isAdmin) this.agent = extension

                    if(!this.startDateTime || !this.endDateTime){
                       alert('Plz input startDateTime or endDateTime')
                       return
                    }

                    this.appendParams["startDateTime"] = this.startDateTime + " 00:00:00"
                    this.appendParams["endDateTime"] = this.endDateTime + " 23:59:59"
                    if(this.agentExtension) this.appendParams["agentExtension"] = this.agentExtension
                    if(this.customerNumber) this.appendParams["customerNumber"] = this.customerNumber
                    this.$nextTick(function(){
                        this.$refs.vuetable.refresh()
                    });

                },
                transformData :function(data){

                    var transformed = {}
                    var pageIndex = data.number
                    var total = data.totalElements
                    var pageSize = data.size
                    var from = pageIndex * pageSize + 1
                    var to = (pageIndex +1 ) * pageSize
                    var lastPage = data.totalPages
                    var currentPage = pageIndex +1
                    transformed.data = data.content
                    transformed.pagination = {
                        total: total,
                        per_page: pageSize,
                        current_page: currentPage,
                        last_page: lastPage,
                        next_page_url: this.url,
                        prev_page_url: this.url,
                        from: from,
                        to: to
                   }

                    return transformed
                },onPaginationData:function(paginationData){
                    this.$refs.pagination.setPaginationData(paginationData);
                    this.$refs.paginationInfo.setPaginationData(paginationData);


                },onChangePage(page){
                  this.$refs.vuetable.changePage(page);
                },onActionClicked:function(action,data){
                   this.showModal = true
                   var _this = this
                   var src = chatRecordDetailPath  + "?id="+ data.callId +"&extension=" + data.agentExtension
                    this.$nextTick (()=>{
                        _this.$refs.iframe.src= src
                    })
                },loadSuccess(res){
                   if(res.request.responseURL.includes('login')){
                         location.href = baseUrl +  "login"
                   }
                }
             },mounted:function(){


                var dt = new Date();
                var year = dt.getFullYear();
                var month = (dt.getMonth()+1).toString().padStart(2, '0')

                //var date = dt.getDate().toString().padStart(2, '0')

                var lastMonth = new Date(year, month, 0).getMonth().toString().padStart(2, '0')
                var lastDayOfMonth = new Date(year, month, 0).getDate();

                this.startDateTime = `${year}-${lastMonth}-01`
                this.endDateTime = `${year}-${month}-${lastDayOfMonth}`



                this.url  = chatRecordMasterPath + "/page/data"
                this.query();




             },created:function(){
                    document.title= this.title;

             }
       })