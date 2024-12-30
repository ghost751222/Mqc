  var app = new Vue({
            "el":"#app",
             data:{
                title:"使用者管理",
                requestPath:null,
                perPage :10,
                baseUrl:null,
                url:null,
                dataPath :"",
                paginationPath :"pagination",
                appendParams:{},
                query_account:"",
                userInfo:{
                     id:null,
                     account:null,
                     password:null,
                     userName:null,
                     extension:null,
                     createTime:null,
                     isAdmin:null
                 },
                showModal: false,
                sortOrder : [],
                fields: [
                    { "name": "id", "title": "id",sortField: "id" },
                    { "name": "account", "title": "Account",sortField: "account" },
                    { "name": "userName", "title": "UserName",sortField: "userName" },
                    { "name": "extension", "title": "Extension",sortField: "extension" },
                    { "name": "isAdmin", "title": "isAdmin",sortField: "isAdmin" ,formatter:function(isAdmin){
                       return isAdmin ? "是" : "否"
                    }},
                    { "name": "actions", "title": "Actions" },

                ],
             },
             methods:{
                selectChange:function(e){
                    var value = e.target.value;
                    var dt = new Date();
                    dt.setDate(dt.getDate() - value)
                    var year = dt.toLocaleDateString("en-us", { year: "numeric" });
                    var month = dt.toLocaleDateString("en-us", { month: "2-digit" });
                    var day = dt.toLocaleDateString("en-us", { day: "2-digit" });
                    var formattedDate = year + "-" + month + "-" + day;
                    this.startTime = formattedDate
                    this.query()
                },
                query:function(e){
                    this.$nextTick(function(){
                        this.$refs.vuetable.refresh()
                    });
                    this.appendParams= {}
                    if(this.query_account) this.appendParams["query_account"] = this.query_account
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

                     this.userInfo.id =data.id
                     this.userInfo.account =data.account
                     this.userInfo.password = data.password
                     this.userInfo.userName =data.userName
                     this.userInfo.extension =data.extension
                     this.userInfo.createTime =data.createTime
                     this.userInfo.isAdmin =data.isAdmin? 1: 0

                if(action =="edit"){
                     this.showModal = true
                }else if(action =="delete"){
                    if(confirm("是否刪除此筆資料")){

                                          axios.delete(this.baseUrl,{data:this.userInfo}).then(res=>{
                                                            this.$refs.vuetable.reload()
                                          }).catch( (error) => {
                                                              alert('delete Data Failed')
                                                              console.error(error)
                                          });
                    }
                }else if(action == "password"){
                    if(confirm("是否重制密碼")){

                                          axios.post(this.baseUrl + "/resetPassword",this.userInfo).then(res=>{
                                                           alert('RePassword Data Successful')
                                          }).catch( (error) => {
                                                              alert('RePassword Data Failed')
                                                              console.error(error)
                                          });
                    }

                }




                },save(){
                   this.userInfo.isAdmin = parseInt( this.userInfo.isAdmin)
                    axios.post( this.baseUrl,this.userInfo).then(res=>{
                      this.$refs.vuetable.reload()
                      this.showModal = false

                    }).catch( (error) => {
                        alert('Save Data Failed')
                        console.error(error)
                    });
                },loadSuccess(res){
                   if(res.request.responseURL.includes('login')){
                         location.href = baseUrl +  "login"
                   }
                },add(){
                   for(i in this.userInfo){
                     this.userInfo[i] = null
                   }
                    this.showModal = true

                }
             },mounted:function(){

                this.baseUrl = requestPath;
                this.url  = requestPath + "/data"
                this.query();

             },created:function(){
                    document.title= this.title;

             }
       })