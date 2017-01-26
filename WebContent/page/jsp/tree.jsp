<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="utf-8"%>
<!DOCTYPE html>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=utf-8">
    <title>ZKB</title>
</head>
<body>
    <div class="alert alert-warning" >
        <div class="row">
            <div class="col-sm-12">
                <strong>ZooKeeperServers:</strong>
                <input type="text" placeholder="127.0.0.1:2181,127.0.0.1:2182,127.0.0.1:2183" id="zkServers" value="" size="100"/>
                <a href="javascript:void(0)" onclick="connectZk()" class="btn btn-primary btn-xs" >Go</a>
            </div>
        </div>
    </div>
    <div class="container-fluid" id="tree">
    </div>
    <div id="TreeModal" class="modal fade" tabindex="1" role="dialog" aria-labelledby="TreeModalLabel" aria-hidden="true"
         style="z-index:79999">
        <div class="modal-dialog modal-lg">
            <div class="modal-content ">
                <div class="modal-header">
                    <button type="button" class="close" data-dismiss="modal" aria-hidden="true">
                        &times;
                    </button>
                    <h4 class="modal-title">
                        <strong id="TreeModalLabel">Content</strong><br/>
                    </h4>
                </div>
                <div class="modal-body">
                    <div class="row">
                        <div class="col-sm-2"><strong>NodePath:</strong></div><div class="col-sm-10" id="realPath"></div>
                    </div>
                    <div class="row">
                        <div class="col-sm-2"><strong>StringValue:</strong></div><div class="col-sm-10" id="stringValue"></div>
                    </div>
                    <div class="row">
                        <div class="col-sm-2"><strong>ObjectValue:</strong></div><div class="col-sm-10" id="objectValue"></div>
                    </div>
                    <div class="row">
                        <div class="col-sm-2"><strong>Base64Value:</strong></div><div class="col-sm-10" id="bytesValue"></div>
                    </div>
                    <div class="row">
                        <div class="col-sm-2"><strong>HexValue:</strong></div><div class="col-sm-10" id="hexValue"></div>
                    </div>
                </div>
            </div>
            <!-- /.modal-content -->
        </div>
        <!-- /.modal -->
    </div>
</body>
<%@ include file="/page/include/include.html"%>
<script>
    var cp = "<%=request.getContextPath()%>";
    var loadTree = function() {
        jQuery.ajax({
            type: "GET",
            url: cp+ "/zkb/tree",
            cache: false,
            contentType: 'application/json',
            dataType: "json",
            data: {
//                base64: base64
            },
            success: function (json) {
                if (json.code == "200") {
                    var tree = "";
                    var space = "";
                    jQuery.each(json.data,function(i,item){
                        var line = "<a href='javascript:void(0)' onclick=readData('"+item.realPath+"')>"+item.viewPath+"</a><br/>";
                        tree += line;
                    });
                    jQuery("#tree").html(tree);
                } else {
                    message(json.msg);
                }
            },
            complete: function (XMLHttpRequest, textStatus) {
            },
            error: function (XMLHttpRequest, textStatus, errorThrown) {
//                alert(XMLHttpRequest.status);
//                alert(XMLHttpRequest.readyState);
//                alert(textStatus);
                message();
            }
        });
    };
    var readData = function (path) {
        jQuery("#realPath").html("");
        jQuery("#bytesValue").html("");
        jQuery("#stringValue").html("");
        jQuery("#objectValue").html("");
        jQuery("#hexValue").html("");
        jQuery.ajax({
            type: "GET",
            url: cp+ "/zkb/readData",
            cache: false,
            contentType: 'application/json',
            dataType: "json",
            data: {
                path: path
            },
            success: function (json) {
                if (json.code == "200") {
                    var zknode = json.data;
                    jQuery("#realPath").html(zknode.realPath);
                    jQuery("#bytesValue").html(zknode.bytesValue);
                    jQuery("#stringValue").html(zknode.stringValue);
                    jQuery("#objectValue").html(zknode.objectValue);
                    jQuery("#hexValue").html(zknode.hexValue);
                    jQuery("#TreeModal").modal("show");
                } else {
                    message(json.msg);
                }
            },
            complete: function (XMLHttpRequest, textStatus) {
            },
            error: function (XMLHttpRequest, textStatus, errorThrown) {
                message();
            }
        });
    };
    var connectZk = function() {
        var zkServers = jQuery("#zkServers").val();
        jQuery("#tree").html("");
        jQuery.ajax({
            type: "GET",
            url: cp+ "/zkb/connectZk",
            cache: false,
            contentType: 'application/json',
            dataType: "json",
            data: {
                zkServers: zkServers
            },
            success: function (json) {
                if (json.code == "200") {
                    loadTree();
                } else {
                    message(json.msg);
                }
            },
            complete: function (XMLHttpRequest, textStatus) {
            },
            error: function (XMLHttpRequest, textStatus, errorThrown) {
                message();
            }
        });
    };
    var message = function (msg) {
        if (msg==undefined || msg==null) {
            msg = "系统错误";
        }
        $.globalMessenger().post({
            showCloseButton: true,
            message: msg
        });
    };
    jQuery(document).ready(function() {
        $._messengerDefaults = {
            extraClasses: 'messenger-fixed messenger-theme-flat messenger-on-bottom messenger-on-right'
        };
        loadTree();
    });
</script>
</html>