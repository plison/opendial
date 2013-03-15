
def getResults(type):
    print "extracting the returns..." 
    import math,os.path
    total = []
    numb = []
    for i in range(1,100):
        filename = type+"/is2013-" + type + "-"+str(i)+".txt"
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
    for i in range(0,len(total)):
        val = total[i] / numb[i]
        line = line + str(val).replace(".", ",") + "\n"
    return line


def getDivergence(type):
    print "extracting the divergence..." 
    import math,os.path
    total = []
    numb = []
    for i in range(1,100):
        filename = type+"/is2013-" + type + "-"+str(i)+".txt"
        if os.path.isfile(filename):
            lines = open(filename).readlines()
            incr = 0
            for line in lines:
                if "K-L divergence" in line:
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


print "UNSTRUCTURED: " + getResults("unstructured").replace("\n", "\t")
print "divergence: " + getDivergence("unstructured").replace("\n", "\t")
#print "LINEAR: " + getResults("linear")
#print "STRUCTURED UNINFORMATIVE: " + getResults("structuninf").replace("\n", "\t")
#print "STRUCTURED divergence: " + getDivergence("structuninf").replace("\n", "\t")
#print "return-2: " + getResults("unstructured2")
#print "divergence-2: " + getDivergence("unstructured2")
#print "divergence-3: " + getDivergence("unstructured3")
#print "return-3: " + getResults("unstructured3")