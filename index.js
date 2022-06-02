var express = require("express"); 
var app = express();
var multer,storage;
multer = require('multer’);

storage = multer.diskStorage({ 
  destination: function(req,file,cb){
    cb(null,'./uploads/');},
  filename: function(req,file,cb){
    cb(null, file.originalname);}
});

app.listen(3000, function(){
  console.log('server running..'); 
});

app.post("/upload",multer({storage: storage}).single('upload'),function(req,res){
  console.log(req.file); 
  console.log(req.body);
res.redirect("/uploads/"+req.file.filename);
  console.log(req.file.filename);
  return res.status(200).end();
});
