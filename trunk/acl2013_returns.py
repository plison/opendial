
def getResults(types):
    print "extracting the returns..." 
    import math,os.path
    total = []
    numb = []
    for type in types:
        for i in range(1,100):
            filename = type+"/acl2013-" + type + "-"+str(i)+".txt"
            if os.path.isfile(filename):
                lines = open(filename).readlines()
                incr = 0
                for line in lines:
                    if "accumulated return" in line and len(line) < 48:
                        ret = line.replace("[Simulator] DEBUG: accumulated return: ", "")
                        if len(total) > incr:
                            total[incr] = total[incr] + float(ret)
                            numb[incr] = numb[incr] +1
                        else:
                            total.append(float(ret))
                            numb.append(1)
                        incr = incr+1
                incr = 0
        
    print numb
    line = ""
    for i in range(0,len(total)/4):
        val = (total[4*i] / numb[4*i] + total[4*i+1] / numb[4*i+1]
               + total[4*i+2] / numb[4*i+2] + total[4*i+3] / numb[4*i+3] )/4.0
        line = line + str(val).replace(".", ",") + "\n"
    return line


def getTimings(types):
    print "extracting the timings..." 
    import math,os.path
    total = []
    numb = []
    for type in types:
        for i in range(1,100):
            counter = 0
            filename = type+"/acl2013-" + type + "-"+str(i)+".txt"
            if os.path.isfile(filename):
                lines = open(filename).readlines()
                incr = 0
                for line in lines:
                    if "last return" in line and len(line) < 44:
                        counter = counter +1
                        if True:
                            ret = line.replace("[Clock] INFO: last return: ", "")
                            if (float(ret)) > 8.0:
                                print filename
                            if len(total) > incr:
                                total[incr] = total[incr] + float(ret)
                                numb[incr] = numb[incr] +1
                            else:
                                total.append(float(ret))
                                numb.append(1)
                            incr = incr+1
                            counter = 0
                incr = 0
        
    line = ""
    for i in range(0,len(total)):
        val = (total[1*i] / numb[1*i])/1.0
        line = line + str(val).replace(".", ",") + "\n"
    print "finished extracting the timings!"
    return ""


def getDivergence(types):
    print "extracting the divergence..." 
    import math,os.path
    total = []
    numb = []
    for type in types:
        for i in range(1,100):
            filename = type+"/acl2013-" + type + "-"+str(i)+".txt"
            if os.path.isfile(filename):
                lines = open(filename).readlines()
                incr = 0
                for line in lines:
                    if "K-L divergence" in line:
                        line = line[line.find("[Simulator]"):]
                        ret = line.replace("[Simulator] DEBUG: K-L divergence: ", "")
                        if "NaN" not in ret and len(total) > incr:
                            total[incr] = total[incr] + float(ret)
                            numb[incr] = numb[incr] +1
                        elif "NaN" not in ret:
                            total.append(float(ret))
                            numb.append(1)
                        incr = incr+1
                incr = 0
           
    line = ""
    for i in range(0,len(total)/10):
        val = 0
        for j in range(0,9):
            val += (total[i*10 + j] / numb[i*10 + j]) 
        val = val / 10.0
        line = line + str(val).replace(".", ",") + "\n"
    return line


#print "MB returns per episode: "  + getResults(["mb"])
#print "MB returns per timings: "  + getTimings(["mb"])
#print "SARSA returns per episode: "  + getResults(["sarsa"])
print "SARSA returns per timings: "  + getTimings(["sarsa"])
#print "SARSA2: "  + getResults(["sarsa2"])
