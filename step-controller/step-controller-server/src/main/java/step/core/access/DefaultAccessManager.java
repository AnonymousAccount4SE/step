package step.core.access;

import java.util.Arrays;
import java.util.List;

import step.core.GlobalContext;

public class DefaultAccessManager implements AccessManager {

	public static List<String> defaultRights = Arrays.asList(new String[]{"interactive","plan-read","plan-write","plan-delete","plan-execute","kw-read","kw-write","kw-delete","kw-execute","report-read","execution-write","execution-delete","user-write","user-read","task-read","task-write","task-delete","admin","param-read","param-write","param-delete","param-global-write","token-manage"});
	
	public static List<String> defaultRoles = Arrays.asList(new String[]{"admin"});
	
	@Override
	public void init(GlobalContext context) {
		
	}

	@Override
	public List<String> getRights(String username) {
		return defaultRights;
	}

	@Override
	public String getRole(String username) {
		return "admin";
	}

	@Override
	public List<String> getRoles() {
		return defaultRoles;
	}

}
