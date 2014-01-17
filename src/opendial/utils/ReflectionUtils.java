// =================================================================                                                                   
// Copyright (C) 2011-2013 Pierre Lison (plison@ifi.uio.no)                                                                            
//                                                                                                                                     
// This library is free software; you can redistribute it and/or                                                                       
// modify it under the terms of the GNU Lesser General Public License                                                                  
// as published by the Free Software Foundation; either version 2.1 of                                                                 
// the License, or (at your option) any later version.                                                                                 
//                                                                                                                                     
// This library is distributed in the hope that it will be useful, but                                                                 
// WITHOUT ANY WARRANTY; without even the implied warranty of                                                                          
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU                                                                    
// Lesser General Public License for more details.                                                                                     
//                                                                                                                                     
// You should have received a copy of the GNU Lesser General Public                                                                    
// License along with this program; if not, write to the Free Software                                                                 
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA                                                                           
// 02111-1307, USA.                                                                                                                    
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
	     * Load all classes from a package.
	     * 
	     * @param packageName
	     * @return
	     * @throws ClassNotFoundException
	     * @throws IOException
	     */
	    public static Class[] getAllClassesFromPackage
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
	        ArrayList<Class> classes = new ArrayList<Class>();
	        for (File directory : dirs) {
	            classes.addAll(findClasses(directory, packageName));
	        }
	        return classes.toArray(new Class[classes.size()]);
	    }

	    /**
	     * Find file in package.
	     * 
	     * @param directory
	     * @param packageName
	     * @return
	     * @throws ClassNotFoundException
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


