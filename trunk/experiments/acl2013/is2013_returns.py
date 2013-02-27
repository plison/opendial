
print "extracting the returns..." 
import math,os.path
total = []
numb = []
for i in range(1,100):
    filename = "linear/is2013-linear-"+str(i)+".txt"
    if os.path.isfile(filename):
        lines = open(filename).readlines()
        incr = 0
        for line in lines:
            if "accumulated return" in line:
                ret = line.replace("[Simulator] DEBUG: accumulated return: ", "")
                if len(total) > incr:
                    total[incr] = total[incr] + float(ret)
                    numb[incr] = numb[incr] +1
                else:
                    total.append(float(ret))
                    numb.append(1)
                incr = incr+1
        incr = 0
    

line = ""
print total
print numb
for i in range(0,len(total)):
    val = total[i] / numb[i]
    line = line + str(val).replace(".", ",") + "\n"
print line