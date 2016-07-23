package step.core.artefacts.handlers;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.DynamicAttribute;
import step.expressions.ExpressionHandler;

public class ArtefactAttributesHandler {

	public static AbstractArtefact evaluateAttributes(AbstractArtefact artefact) {
		AbstractArtefact result = (AbstractArtefact) cloneBean(artefact);
		ExpressionHandler expressionHandler = new ExpressionHandler();
		processResolvableParameter(result, expressionHandler);
		return result;
	}
	
	private static void processResolvableParameter(Object o, ExpressionHandler expressionHandler) {
		if(o!=null) {
			Class<?> clazz = o.getClass();
			do {
				for(Field field:clazz.getDeclaredFields()) {
					DynamicAttribute command = field.getAnnotation(DynamicAttribute.class);
					if(command!=null) {
						try {
							field.setAccessible(true);
							Object object = field.get(o);
							if(object instanceof String) {
								String string = (String) object;
								String newString = expressionHandler.evaluateAttributeParameter(string);
								field.set(o, newString);
							} else {	
								processResolvableParameter(object, expressionHandler);
							}
						} catch (IllegalArgumentException | IllegalAccessException e) {
							throw new RuntimeException(e);
						}
					}
				}
				clazz = clazz.getSuperclass();
			} while (clazz != Object.class);
		}
	}
	
	private static Object cloneBean(Object in) {
		try {
			Object out = in.getClass().newInstance();
			Class<?> clazz = in.getClass();
			do {
				for(Field field:clazz.getDeclaredFields()) {
					if(!Modifier.isStatic(field.getModifiers())) {
						field.setAccessible(true);
						Object object = field.get(in);
						if(object instanceof String) {
							String string = (String) object;
							field.set(out, new String(string));
						} else {
							field.set(out, object);
						}
					}
				}
				clazz = clazz.getSuperclass();
			} while (clazz != Object.class);
			
			return out;
		} catch (IllegalArgumentException | IllegalAccessException | InstantiationException e) {
			throw new RuntimeException(e);
		}
	}
}
