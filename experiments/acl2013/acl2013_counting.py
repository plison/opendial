
files = ["eman_moves.txt", "erik_moves.txt", "gordana_moves.txt", 
         "jantore_moves.txt", "marte_moves.txt", "murhaf_moves.txt", 
         "rebecca_moves.txt", "stig_moves.txt"]

def getSequence(file):
    lines = open(file).readlines()
    sequence = []
    for l in lines:
        l = l.replace('\n', '')
        if len(l.split(" = ")) == 2:
            entry = {}
            entry['var'] =  l.split(" = ")[0]
            entry['content'] = l.split(" = ")[1]
            sequence.append(entry)
    return sequence

def normalise(table):
    total = 0.0
    for row in table.keys():
        total += table[row]
    if total > 0:
        for row in table.keys():
            table[row] = table[row] / total
    return table

def getIuPriors(sequences):
    counts = {}
    for seq in sequences:
        for s in seq:
            if s['var'] == 'i_u':
                cont = s['content']
                cont = cont.replace('(yellow)', '')
                cont = cont.replace('(green)', '')
                cont = cont.replace('(blue)', '')
                if "AskConfirm" in cont:
                    cont = "AskConfirm"
                if counts.has_key(cont):
                    counts[cont] = counts[cont] + 1.0
                else:
                    counts[cont] = 1.0
    return normalise(counts)

def getPickUpPriors(sequences):
    counts = {}
    for seq in sequences:
        carried = []
        for s in seq:
            if s['var'] == 'carried' and len(s['content']) > 2:
                carried = s['content'][1:len(s['content'])-1].split(',')
            if s['var'] == 'i_u' and len(carried) == 0:
                cont = s['content']
                cont = cont.replace('(yellow)', '')
                cont = cont.replace('(green)', '')
                cont = cont.replace('(blue)', '')
                if "AskConfirm" in cont:
                    cont = "AskConfirm"
                if counts.has_key(cont):
                    counts[cont] = counts[cont] + 1.0
                else:
                    counts[cont] = 1.0
    return normalise(counts)

def getReleasePriors(sequences):
    counts = {}
    for seq in sequences:
        carried = []
        for s in seq:
            if s['var'] == 'carried' and len(s['content']) > 2:
                carried = s['content'][1:len(s['content'])-1].split(',')
            if s['var'] == 'i_u' and len(carried) > 0:
                cont = s['content']
                cont = cont.replace('(yellow)', '')
                cont = cont.replace('(green)', '')
                cont = cont.replace('(blue)', '')
                if "AskConfirm" in cont:
                    cont = "AskConfirm"
                if counts.has_key(cont):
                    counts[cont] = counts[cont] + 1.0
                else:
                    counts[cont] = 1.0
    return normalise(counts)

def getLast (var, seq, index):
    for j in range (index, 0, -1):
        if seq[j]['var'] == var:
            return seq[j]['content']
    return None


def getNextAu(seq, index):
    j = index+1
    nextAu = None
    while j < len(seq):
        if seq[j]['var']== "a_u":
            nextAu = seq[j]['content']
            j = 300
        elif seq[j]['var'] == "i_u":
            j = 300
        j = j+1
    return nextAu

def getUserActions_case1(sequences):
    actions = {}
    actions['SAME'] = 0
    actions['CONF'] = 0
    actions['DISCONF'] = 0
    actions['OTHER'] = 0
    for seq in sequences:
        for i in range (0, len(seq)):
            s = seq[i]
            if s['var'] == 'a_m' and "Ground" in s['content']:
                grounded = s['content'][s['content'].find('(')+1:len(s['content'])-1]
                
                lastIu = getLast('i_u', seq, i)
                nextAu = getNextAu(seq, i)
                    
                if grounded == lastIu:
                    if nextAu == lastIu:
                        actions['SAME'] = actions['SAME'] + 1
                    elif nextAu == "Disconfirm":
                        actions['DISCONF'] = actions['DISCONF'] + 1
                    elif nextAu == "Confirm":
                        actions['CONF'] = actions['CONF'] + 1
                    else:
                        actions['OTHER'] = actions['OTHER'] + 1
                        
    normalise(actions)
    return actions


def getUserActions_case2(sequences):
    actions = {}
    actions['SAME'] = 0
    actions['DISCONF'] = 0
    actions['OTHER'] = 0
    for seq in sequences:
        for i in range (0, len(seq)):
            s = seq[i]
            if s['var'] == 'a_m' and "Ground" in s['content']:
                grounded = s['content'][s['content'].find('(')+1:len(s['content'])-1]
                
                lastIu = getLast('i_u', seq, i)
                nextAu = getNextAu(seq, i)
                    
                if grounded != lastIu:
                    if nextAu == lastIu:
                        actions['SAME'] = actions['SAME'] + 1
                    elif nextAu == "Disconfirm":
                        actions['DISCONF'] = actions['DISCONF'] + 1
                    else:
                        actions['OTHER'] = actions['OTHER'] + 1
                        
    normalise(actions)
    return actions
                  
def getUserActions_case3(sequences):
    actions = {}
    actions['SAME'] = 0
    actions['CONF'] = 0
    actions['DISCONF'] = 0
    actions['OTHER'] = 0
    for seq in sequences:
        for i in range (0, len(seq)):
            s = seq[i]
            if s['var'] == 'a_m' and "AskConfirm" in s['content']:
                grounded = s['content'][s['content'].find('(')+1:len(s['content'])-1]
                
                lastIu = getLast('i_u', seq, i)
                nextAu = getNextAu(seq, i)
                    
                if grounded == lastIu:
                    if nextAu == lastIu:
                        actions['SAME'] = actions['SAME'] + 1
                    elif nextAu == "Disconfirm":
                        actions['DISCONF'] = actions['DISCONF'] + 1
                    elif nextAu == "Confirm":
                        actions['CONF'] = actions['CONF'] + 1
                    else:
                        actions['OTHER'] = actions['OTHER'] + 1
                        
    normalise(actions)
    return actions


def getUserActions_case4(sequences):
    actions = {}
    actions['SAME'] = 0
    actions['CONF'] = 0
    actions['DISCONF'] = 0
    actions['OTHER'] = 0
    for seq in sequences:
        for i in range (0, len(seq)):
            s = seq[i]
            if s['var'] == 'a_m' and "AskConfirm" in s['content']:
                grounded = s['content'][s['content'].find('(')+1:len(s['content'])-1]
                
                lastIu = getLast('i_u', seq, i)
                nextAu = getNextAu(seq, i)
                    
                if grounded != lastIu:
                    if nextAu == lastIu:
                        actions['SAME'] = actions['SAME'] + 1
                    elif nextAu == "Disconfirm":
                        actions['DISCONF'] = actions['DISCONF'] + 1
                    elif nextAu == "Confirm":
                        actions['CONF'] = actions['CONF'] + 1
                    else:
                        actions['OTHER'] = actions['OTHER'] + 1
                        
    normalise(actions)
    return actions

def getUserActions_case5(sequences):
    actions = {}
    actions['REPLAST'] = 0
    actions['SAME'] = 0
    actions['OTHER'] = 0
    for seq in sequences:
        for i in range (0, len(seq)):
            s = seq[i]
            if s['var'] == 'i_u':
                
                lastAm = str(getLast('a_m', seq, i))
                actionContent = lastAm[lastAm.find('(')+1:len(lastAm)-1]

                nextAu = getNextAu(seq, i)
                    
                if actionContent == s['content']:
                    if nextAu == "RepeatLast":
                        actions['REPLAST'] = actions['REPLAST'] + 1
                    elif nextAu == s['content']:
                        actions['SAME'] = actions['SAME'] + 1
                    else:
                        actions['OTHER'] = actions['OTHER'] + 1
                        
    normalise(actions)
    return actions

def getUserActions_case6(sequences):
    actions = {}
    actions['SAME'] = 0
    actions['OTHER'] = 0
    for seq in sequences:
        for i in range (0, len(seq)):
            s = seq[i]
            if s['var'] == 'i_u':
                
                lastAm = str(getLast('a_m', seq, i))
                actionContent = lastAm[lastAm.find('(')+1:len(lastAm)-1]
                nextAu = getNextAu(seq, i)
                if actionContent != s['content']:
                    if nextAu == s['content']:
                        actions['SAME'] = actions['SAME'] + 1
                    else:
                        actions['OTHER'] = actions['OTHER'] + 1
                        
    normalise(actions)
    return actions


#OBSERVATION PARAMS:  5.5509    3.4113

sequences = []
for f in files:
    sequences.append(getSequence(f))

#print getPickUpPriors(sequences)
#print getReleasePriors(sequences)
#print getIuPriors(sequences)
print getUserActions_case1(sequences)
print getUserActions_case2(sequences)
print getUserActions_case3(sequences)
print getUserActions_case4(sequences)
print getUserActions_case5(sequences)
print getUserActions_case6(sequences)
    
