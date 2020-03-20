package step.plugins.views.functions;

import java.util.HashMap;
import java.util.Map;

import step.artefacts.reports.CallFunctionReportNode;
import step.core.artefacts.reports.ReportNode;
import step.core.artefacts.reports.ReportNodeStatus;
import step.plugins.views.AbstractView;
import step.plugins.views.View;

@View
public class ReportNodeStatusDistributionView extends AbstractView<ReportNodeStatusDistribution> {	

	@Override
	public void afterReportNodeSkeletonCreation(ReportNodeStatusDistribution model, ReportNode node) {
		if(node instanceof CallFunctionReportNode && node.persistNode()) {
			model.countForecast++;
		}
	}

	@Override
	public void afterReportNodeExecution(ReportNodeStatusDistribution model, ReportNode node) {
		if(node instanceof CallFunctionReportNode && node.persistNode()) {
			model.distribution.get(node.getStatus()).count++;
			model.count++;
			if(model.countForecast<model.count) {
				model.countForecast=model.count;
			}
		}
	}

	@Override
	public ReportNodeStatusDistribution init() {
		Map<ReportNodeStatus, ReportNodeStatusDistribution.Entry> progress = new HashMap<>();
		for(ReportNodeStatus status:ReportNodeStatus.values()) {
			progress.put(status, new ReportNodeStatusDistribution.Entry(status));
		}
		ReportNodeStatusDistribution distribution = new ReportNodeStatusDistribution(progress);
		distribution.setLabel("Keyword calls: ");
		return distribution;
	}

	@Override
	public String getViewId() {
		return "statusDistributionForFunctionCalls";
	}

	@Override
	public void rollbackReportNode(ReportNodeStatusDistribution model, ReportNode node) {
		if(node instanceof CallFunctionReportNode && node.persistNode()) {
			model.distribution.get(node.getStatus()).count--;
			model.count--;
			model.countForecast--;
		}
		
	}
}
