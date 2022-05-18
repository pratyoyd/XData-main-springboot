import json
import requests
f =open('/home/pratyoy/Downloads/Xdata-main-springboot (copy)/src/main/java/test/universityTest/apiinput.json')
data = json.load(f)
f.close()
url = "http://localhost:8080/test/getxdataoutput"
headers = {'Content-type': 'application/json', 'Accept': 'application/json'}
r = requests.post(url, data=json.dumps(data), headers=headers)
print(r.json())
# Iterating through the json
# list

# Closing file

