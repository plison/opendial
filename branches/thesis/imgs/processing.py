import matplotlib.pyplot as plt
import numpy as np
import matplotlib.mlab as mlab
from math import *

fig = plt.figure()
ax = fig.add_subplot(111)

mean = 20
variance = 50
sigma = sqrt(variance)
x = np.linspace(-15,40,100)
ax.plot(x,mlab.normpdf(x,mean,sigma), linewidth=2)
ax.set_xlabel('Temperature', fontsize=17, fontweight='bold')
ax.set_yticks([]) 
ax.set_xticks([]) 
ax.set_ylabel('Probability density', fontsize=17, fontweight='bold')
plt.show()