package step.artefacts.handlers;

import java.util.Map;

import step.artefacts.Check;
import step.core.artefacts.handlers.ArtefactHandler;
import step.core.artefacts.reports.ReportNode;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.execution.ExecutionContext;
import step.expressions.ExpressionHandler;

public class CheckHandler extends ArtefactHandler<Check, ReportNode> {
	
	@Override
	protected void createReportSkeleton_(ReportNode parentNode, Check testArtefact) {

	}

	@Override
	protected void execute_(ReportNode node, Check testArtefact) {
		ExpressionHandler expressionHandler = new ExpressionHandler();
		Map<String, Object> bindings = ExecutionContext.getCurrentContext().getVariablesManager().getAllVariables();
		
		Object result = expressionHandler.evaluateGroovyExpression(testArtefact.getExpression(), bindings);

		if(result!=null) {
			Boolean resultBoolean;
			if(result instanceof Boolean) {
				resultBoolean = (Boolean) result;
			} else if (result instanceof String) {
				try {
					resultBoolean = Boolean.parseBoolean((String)result);
				} catch (Exception e) {
					node.setError("The check expression didn't return a boolean");
					node.setStatus(ReportNodeStatus.TECHNICAL_ERROR);
					return;
				} 				
			} else {
				node.setError("The check expression returned an instance of "+result.getClass().getName());
				node.setStatus(ReportNodeStatus.TECHNICAL_ERROR);
				return;
			}
			if(resultBoolean) {
				node.setStatus(ReportNodeStatus.PASSED);
			} else {
				node.setStatus(ReportNodeStatus.FAILED);
			}
		} else {
			node.setError("The check expression is null");
			node.setStatus(ReportNodeStatus.TECHNICAL_ERROR);
			return;
		}
	}

	@Override
	public ReportNode createReportNode_(ReportNode parentNode, Check testArtefact) {
		return new ReportNode();
	}
}
