package step.grid.agent.handler;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TokenHandlerPool {

	private Map<String, MessageHandler> pool = new HashMap<>();
	
	public MessageHandler get(String handlerKey) throws Exception {
		MessageHandler handler = createHandler(handlerKey); //pool.get(handlerKey); 
		
		if(handler==null) {
			handler = createHandler(handlerKey);
			pool.put(handlerKey, handler);
		}

		return handler;
	}

	private static final String DELIMITER = "\\|";
	
	private MessageHandler createHandler(String handlerChain) throws Exception {
		String[] handlers = handlerChain.split(DELIMITER); 

		MessageHandler previous = null;
		for(int i=handlers.length-1;i>=0;i--) {
			String handlerKey = handlers[i];
			previous =  createHandler_(handlerKey, previous);
		}
		
		return previous;
	}

	private MessageHandler createHandler_(String handlerKey, MessageHandler previous) throws ReflectiveOperationException, MalformedURLException,
			ClassNotFoundException, InstantiationException, IllegalAccessException {
		MessageHandler handler;
		Matcher m = HANDLER_KEY_PATTERN.matcher(handlerKey);
		if(m.matches()) {
			String factory = m.group(1);
			String factoryKey = m.group(2);
			
			if(factory.equals("class")) {
				try {
					Class<?> class_ = Class.forName(factoryKey);
					handler = newInstance(class_);
				} catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
					throw e;
				}
			} else if (factory.equals("classuri")) {		
				//JarClassLoader jcl = new JarClassLoader();
				//jcl.add(factoryKey);			
				List<URL> urls = new ArrayList<>();
				File f = new File(factoryKey);
				if(f.isDirectory()) {
					for(File file:f.listFiles()) {
						if(file.getName().endsWith(".jar")) {
							urls.add(file.toURI().toURL());
						}
					}
				}
				urls.add(f.toURI().toURL());
				
				//(new URLClassLoader(new URL[]{new File("D:/Workspace/step/selenium-scripts-oam/target/classes/selenium-api-2.53.1.jar").toURI().toURL()})).loadClass("org.openqa.selenium.WebDriver")
				
				ClassLoader cl = new URLClassLoader(urls.toArray(new URL[urls.size()]), Thread.currentThread().getContextClassLoader());
				handler = new ClassLoaderMessageHandlerWrapper(cl);
			} else {
				throw new RuntimeException("Unknown handler factory: "+factory);
			}
			
			if(handler instanceof MessageHandlerDelegate) {
				((MessageHandlerDelegate)handler).setDelegate(previous);
			}
				
		} else {
			throw new RuntimeException("Invalid handler key: "+handlerKey);
		}
		return handler;
	}

	private MessageHandler newInstance(Class<?> class_)
			throws InstantiationException, IllegalAccessException {
		Object o = class_.newInstance();
		if(o!=null && o instanceof MessageHandler) {
			return (MessageHandler)o;
		} else {
			throw new RuntimeException("The class '"+class_.getName()+"' doesn't extend "+MessageHandler.class);
		}
	}
	
	private static final Pattern HANDLER_KEY_PATTERN = Pattern.compile("(.+?):(.+?)");
}
