(window["webpackJsonp"]=window["webpackJsonp"]||[]).push([["service-fun"],{"001c":function(e,t,n){},"15fd":function(e,t,n){"use strict";n.d(t,"a",(function(){return r}));n("a4d3"),n("c975"),n("b64b");function a(e,t){if(null==e)return{};var n,a,r={},i=Object.keys(e);for(a=0;a<i.length;a++)n=i[a],t.indexOf(n)>=0||(r[n]=e[n]);return r}function r(e,t){if(null==e)return{};var n,r,i=a(e,t);if(Object.getOwnPropertySymbols){var o=Object.getOwnPropertySymbols(e);for(r=0;r<o.length;r++)n=o[r],t.indexOf(n)>=0||Object.prototype.propertyIsEnumerable.call(e,n)&&(i[n]=e[n])}return i}},"3f86":function(e,t,n){"use strict";n("92fd")},9162:function(e,t,n){"use strict";n("001c")},"92fd":function(e,t,n){},"9cc3":function(e,t,n){"use strict";n.r(t);var a=function(){var e=this,t=e.$createElement,n=e._self._c||t;return n("div",{staticClass:"flex flex-column tab-content"},[n("yl-form-collapse",{attrs:{"collapse-position":"right","collapse-height":"42px"}},[n("template",{slot:"form"},[n("yl-form",e._b({attrs:{size:"mini"},model:{value:e.form,callback:function(t){e.form=t},expression:"form"}},"yl-form",e.formProps,!1))],1),n("template",{slot:"collapse"},[n("el-row",{staticClass:"flex flex-row justify-end"},[n("el-button",{attrs:{size:"small",type:"primary"},on:{click:e.onQuery}},[e._v(" 查询 ")]),n("el-button",{attrs:{size:"small"},on:{click:e.onReset}},[e._v(" 清除 ")])],1)],1)],2),n("div",{staticClass:"btn_groups"},[n("el-button",{attrs:{size:"small",type:"primary"},on:{click:e.handleAdd}},[e._v(" 新增 ")])],1),n("yl-table",e._b({directives:[{name:"loading",rawName:"v-loading",value:e.loading.fetchData,expression:"loading.fetchData"}],scopedSlots:e._u([{key:"body(shippingOrder)",fn:function(t){return[n("span",{staticClass:"hover_green_wj"},[e._v(e._s(t.row.shippingOrder))])]}},{key:"body(action)",fn:function(t){var a=t.row,r=t.index;return[n("el-button",{attrs:{type:"text"},on:{click:function(t){return e.handleEdit(a)}}},[e._v(" 编辑 ")]),!0===a.status?n("el-button",{attrs:{type:"text"},on:{click:function(t){return e.handleEnable(a,!1)}}},[e._v(" 停用 ")]):n("el-button",{attrs:{type:"text"},on:{click:function(t){return e.handleEnable(a,!0)}}},[e._v(" 启用 ")]),n("el-button",{attrs:{type:"text"},on:{click:function(t){return e.handleDel(r)}}},[e._v(" 删除 ")]),n("el-button",{attrs:{type:"text"},on:{click:function(t){return e.handleView(a)}}},[e._v(" 查看流程 ")])]}}])},"yl-table",e.tableProps,!1)),n("addOrEditDialog",{ref:"addOrEditDialog",on:{success:e.handleSearch}})],1)},r=[],i=(n("c975"),n("a434"),n("15fd")),o=(n("96cf"),n("1da1")),l=n("5530"),s=n("854d"),c=n.n(s),u=n("89b1"),f=n.n(u),d=[{name:"功能名称",key:"funName",attrs:{clearable:!0}},{name:"流程名称",key:"flowName",attrs:{clearable:!0}}],m={false:"关闭",true:"开启"},p=[{key:"order",type:"index",name:"序号",fixed:!0,width:60},{key:"action",name:"操作",fixed:!0,width:220},{key:"funName",name:"功能名称",width:100},{key:"appName",name:"归属应用",width:180},{key:"flowName",name:"流程名称",width:180},{key:"abnormalRate",name:"异常率",width:180},{key:"status",name:"状态",width:180,render:function(e){return m[e]}},{key:"version",name:"版本号",width:180},{key:"desc",name:"说明"}],h=function(){var e=this,t=e.$createElement,n=e._self._c||t;return n("el-dialog",{directives:[{name:"dialogDrag",rawName:"v-dialogDrag"}],attrs:{title:e.title,visible:e.visible,width:"30%","append-to-body":"","close-on-click-modal":!1},on:{"update:visible":function(t){e.visible=t},close:e.hide}},[n("div",{staticClass:"dialog_content adjust-box"},[n("el-form",{ref:"form",attrs:{model:e.form,"label-width":"120px",rules:e.rules,"label-position":"right"}},[n("el-form-item",{attrs:{label:"功能名称",prop:"funName"}},[n("el-input",{attrs:{placeholder:"请输入功能名称",maxlength:"30"},model:{value:e.form.funName,callback:function(t){e.$set(e.form,"funName","string"===typeof t?t.trim():t)},expression:"form.funName"}})],1),n("el-form-item",{attrs:{label:"归属应用",prop:"appName"}},[n("el-input",{attrs:{placeholder:"请输入应用名称",maxlength:"30"},model:{value:e.form.appName,callback:function(t){e.$set(e.form,"appName","string"===typeof t?t.trim():t)},expression:"form.appName"}})],1),n("el-form-item",{attrs:{label:"流程名称",prop:"flowName"}},[n("el-input",{attrs:{placeholder:"请输入流程名称",maxlength:"30"},model:{value:e.form.flowName,callback:function(t){e.$set(e.form,"flowName","string"===typeof t?t.trim():t)},expression:"form.flowName"}})],1),n("el-form-item",{attrs:{label:"版本号",prop:"version"}},[n("el-input",{attrs:{placeholder:"请输入版本号",maxlength:"30"},model:{value:e.form.version,callback:function(t){e.$set(e.form,"version","string"===typeof t?t.trim():t)},expression:"form.version"}})],1),n("el-form-item",{attrs:{label:"说明",prop:"desc"}},[n("el-input",{attrs:{maxlength:"60",type:"textarea","show-word-limit":""},model:{value:e.form.desc,callback:function(t){e.$set(e.form,"desc","string"===typeof t?t.trim():t)},expression:"form.desc"}})],1)],1)],1),n("div",{staticClass:"dialog-footer",attrs:{slot:"footer"},slot:"footer"},[n("el-button",{on:{click:e.cancel}},[e._v(" 取 消 ")]),n("el-button",{attrs:{type:"primary",loading:e.loading.confirm},on:{click:e.confirm}},[e._v(" 确 定 ")])],1)])},b=[],g={mixins:[f.a],data:function(){return{isAdd:!0,loading:{confirm:!1},visible:!1,form:{},rules:{funName:[{required:!0,message:"请输入功能名称",trigger:"blur"}],appName:[{required:!0,message:"请输入应用名称",trigger:"blur"}],flowName:[{required:!0,message:"请输入流程名称",trigger:"blur"}],version:[{required:!0,message:"请输入版本号",trigger:"blur"}],desc:[{required:!0,message:"请输入说明",trigger:"blur"}]}}},computed:{title:function(){return this.isAdd?"新增":"编辑"}},mounted:function(){},created:function(){},methods:{fetchData:function(){},confirm:function(){var e=this;return Object(o["a"])(regeneratorRuntime.mark((function t(){var n;return regeneratorRuntime.wrap((function(t){while(1)switch(t.prev=t.next){case 0:return t.next=2,e.$refs.form.validate();case 2:n=Object(l["a"])({},e.form),e.$emit("success",n),e.hide();case 5:case"end":return t.stop()}}),t)})))()},cancel:function(){this.hide()},show:function(){this.isAdd=!0,this.form={},this.visible=!0},showEdit:function(e){this.isAdd=!1,this.form=Object(l["a"])({},e),this.visible=!0},hide:function(){this.$refs.form.resetFields(),this.visible=!1}}},y=g,v=(n("3f86"),n("2877")),w=Object(v["a"])(y,h,b,!1,null,"2308ffca",null),x=w.exports,k={name:"FunManage",components:{addOrEditDialog:x},mixins:[c.a,f.a],data:function(){return{form:{},tableData:[{},{flowName:"xxxx"}],tableHeight:"60vh",shippingOrderDetails:{visible:!1,item:{}},loading:{fetchData:!1}}},computed:{formProps:function(){var e=this;return{fields:d,fieldCol:{xl:6,lg:6,md:8},wrapperWidth:"80%",total:10,nextFields:[{key:"funName",nativeOn:{keyup:this.handleKeyUpQuery},on:{clear:function(){e.onQuery()}}},{key:"flowName",nativeOn:{keyup:this.handleKeyUpQuery},on:{clear:function(){e.onQuery()}}}]}},tableProps:function(){return Object(l["a"])({autofit:!1,tableHeaderKey:"funManage",height:this.tableHeight,fields:p,border:!0,total:this.total,data:this.tableData,pageSizeKey:"size",currentKey:"page",pageParam:{size:10,page:1}},this.mixinTableProps)}},mounted:function(){var e=this;setTimeout((function(){e.tableHeight="calc(100vh - 290px)"}),50)},methods:{handleView:function(){window.open("#/dag?nodeId=1")},handleEnable:function(e,t){var n=e;n.status=t,this.tableData.splice(this.tableData.indexOf(e),1,n),this.onQuery()},handleDel:function(e){var t=this;this.$confirm("确定删除?","提示",{confirmButtonText:"确定",cancelButtonText:"取消",type:"warning"}).then((function(){t.tableData.splice(e,1)}))},handleSearch:function(e){this.tableData.push(e)},handleAdd:function(){this.$refs.addOrEditDialog.show()},handleEdit:function(e){this.$refs.addOrEditDialog.showEdit(e)},fetchData:function(e){var t=this;return Object(o["a"])(regeneratorRuntime.mark((function n(){var a,r;return regeneratorRuntime.wrap((function(n){while(1)switch(n.prev=n.next){case 0:a=Object(l["a"])({},e),a.page,a.size,r=Object(i["a"])(a,["page","size"]),console.log(r),console.log(t.tableData),t.total=t.tableData.length;case 5:case"end":return n.stop()}}),n)})))()},handleKeyUpQuery:function(e){var t=e.keyCode;13===t&&this.onQuery()}}},O=k,N=(n("9162"),Object(v["a"])(O,a,r,!1,null,"4b47bd13",null));t["default"]=N.exports}}]);
//# sourceMappingURL=service-fun.9f0328d2.js.map