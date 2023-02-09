var appid = "Sg8";
var ukey = "03e1923c";
var authType = "3";
var usecret = "442";
var time = (new Date()).getTime().toString();//获取当前时间戳
pm.environment.set('time', time);

var str = 'appid=' + appid + '&authType=' + authType + '&time=' + time + '&ukey=' + ukey;

var bodyStr = ukey + str + usecret;
var sign = CryptoJS.MD5(bodyStr).toString();
pm.environment.set('sign', sign);//加密后的密码字符串赋值给环境变量


//获取body中参数，上面代码可以进行改造
var body = pm.request.body.raw
var body_json = JSON.parse(body)
pwd = body_json["password"]
console.log(pwd)  //在console打印pwd参数
//获取header中的参数
pm.request.headers.get("Cookie")

