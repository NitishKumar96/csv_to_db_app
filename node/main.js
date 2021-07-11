const express = require('express');
const upload = require('express-fileupload')
const exec = require('child_process').exec

const app = express();
const router = express.Router();
app.use(upload())

router.use((req, res, next) => {
  console.log('Time:', Date.now());
  next();
});

router.get('/csv', (req, res) => {
  const path = __dirname + "/view/index.html";
  res.sendFile(path)
});

router.post('/new_file', (req, res) => {
  if (req.files) {
    var csv_file = req.files.csv_file
    const upload_path = __dirname + "/uploads/"
    var new_name = Date.now() + "_" + csv_file.name;
    console.log(csv_file.name + " saved as " + new_name)
    csv_file.mv(upload_path + new_name, function (err) {
      if (err) {
        res.send(err)
      } else {
        var jar_command = "java -jar "+__dirname+"/read_app.jar -f "+__dirname+"/uploads/" + new_name
        if (req.body.table_name.length > 0) {
          jar_command = jar_command + " -t " + req.body.table_name;
        }
        if (req.body.thread_count.length > 0) {
          if (parseInt(req.body.thread_count) > 0) {
            jar_command = jar_command + " --thread_count " + req.body.thread_count;
          }
        }
        if (req.body.process_index.length > 0) {
          if (parseInt(req.body.thread_count) > -1) {
            jar_command = jar_command + " -ai " + req.body.process_index;
          }
        }
        if (req.body.batch_size.length > 0) {
          if (parseInt(req.body.thread_count) > 0) {
            jar_command = jar_command + " --batch_size " + req.body.batch_size;
          }
        }
        console.log(jar_command)
        child = exec(jar_command,
          function (error, stdout, stderr) {
            console.log('stdout: ' + stdout);
            console.log('stderr: ' + stderr);
            if (error !== null) {
              console.log('exec error: ' + error);
            }
          });
        res.send("File uploaded.")
      }
    })
  }
})

app.use('/', router);
app.listen(process.env.port || 3000);

console.log('Web Server is listening at port ' + (process.env.port || 3000));