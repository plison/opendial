// =================================================================                                                                   
// Copyright (C) 2011-2013 Pierre Lison (plison@ifi.uio.no)                                                                            
// Permission is hereby granted, free of charge, to any person 
// obtaining a copy of this software and associated documentation 
// files (the "Software"), to deal in the Software without restriction, 
// including without limitation the rights to use, copy, modify, merge, 
// publish, distribute, sublicense, and/or sell copies of the Software, 
// and to permit persons to whom the Software is furnished to do so, 
// subject to the following conditions:

// The above copyright notice and this permission notice shall be 
// included in all copies or substantial portions of the Software.

// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, 
// EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
// MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. 
// IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY 
// CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, 
// TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE 
// SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
// =================================================================                                                                   

package opendial.utils;


import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import opendial.arch.Logger;

/**
 * Modified from http://oohhyeah.blogspot.no/2012/01/use-java-reflection-to-find-classes.html
 * 
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 */
public class ReflectionUtils {

	// logger
	public static Logger log = new Logger("ReflectionUtils", Logger.Level.NORMAL);

	    @SuppressWarnings("unchecked")
		public static <T> Map<String,Class<T>> findImplementingClasses
	    (final Class<T> interfaceClass, final Package fromPackage) {

	        if (interfaceClass == null) {
	            log.warning("Unknown subclass.");
	            return null;
	        }

	        if (fromPackage == null) {
	            log.warning("Unknown package.");
	            return null;
	        }

	        final Map<String,Class<T>> rVal = new HashMap<String,Class<T>>();
	        try {
	            final Class<?>[] targets = getAllClassesFromPackage(fromPackage.getName());
	            if (targets != null) {
	                for (Class<?> aTarget : targets) {
	                    if (aTarget == null) {
	                        continue;
	                    }
	                    else if (aTarget.equals(interfaceClass)) {
	                        continue;
	                    }
	                    else if (!interfaceClass.isAssignableFrom(aTarget)) {
	                        continue;
	                    }
	                    else if (aTarget.isInterface()) {
	                    	continue;
	                    }
	                    else {
	                        rVal.put(aTarget.getSimpleName(),(Class<T>)aTarget);
	                    }
	                }
	            }
	        }
	        catch (ClassNotFoundException e) {
	            log.warning("Error reading package name.");
	        }
	        catch (IOException e) {
	            log.warning("Error reading classes in package.");
	        }

	        return rVal;
	    }

	    /**
	     * Return all classes from a package.
	     * 
	     * @param packageName the package name
	     * @return all classes from the package
	     * @throws ClassNotFoundException if no class if found
	     * @throws IOException I/O error
	     */
	    public static Class<?>[] getAllClassesFromPackage
	    (final String packageName) throws ClassNotFoundException, IOException {
	    	
	        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
	        assert classLoader != null;
	        String path = packageName.replace('.', '/');
	        Enumeration<URL> resources = classLoader.getResources(path);
	        List<File> dirs = new ArrayList<File>();
	        while (resources.hasMoreElements()) {
	            URL resource = resources.nextElement();
	            dirs.add(new File(resource.getFile()));
	        }
	        ArrayList<Class<?>> classes = new ArrayList<Class<?>>();
	        for (File directory : dirs) {
	            classes.addAll(findClasses(directory, packageName));
	        }
	        return classes.toArray(new Class[classes.size()]);
	    }

	    /**
	     * Find file in package.
	     * 
	     * @param directory the directory
	     * @param packageName the package name
	     * @return the list of found classes
	     * @throws ClassNotFoundException if the class is not found
	     */
	    public static List<Class<?>> findClasses(File directory, String packageName) throws ClassNotFoundException {
	        List<Class<?>> classes = new ArrayList<Class<?>>();
	        if (!directory.exists()) {
	            return classes;
	        }
	        File[] files = directory.listFiles();
	        for (File file : files) {
	            if (file.isDirectory()) {
	                assert !file.getName().contains(".");
	                classes.addAll(findClasses(file, packageName + "." + file.getName()));
	            }
	            else if (file.getName().endsWith(".class")) {
	                classes.add(Class.forName(packageName + '.' + file.getName().substring(0, file.getName().length() - 6)));
	            }
	        }
	        return classes;
	    }
	}


