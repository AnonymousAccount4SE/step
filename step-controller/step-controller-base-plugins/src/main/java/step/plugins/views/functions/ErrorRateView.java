package step.plugins.views.functions;

import java.util.Map.Entry;

import step.core.artefacts.reports.ReportNode;
import step.plugins.views.View;

@View
public class ErrorRateView extends AbstractTimeBasedView<ErrorRateEntry> {

	@Override
	public void afterReportNodeSkeletonCreation(AbstractTimeBasedModel<ErrorRateEntry> model, ReportNode node) {}
	
	private ErrorRateEntry createPoint(ReportNode node) {
		ErrorRateEntry e = null;
		if(node.getError()!=null && node.persistNode()) {
			e = new ErrorRateEntry();
			e.count = 1;
			e.countByErrorMsg.put(node.getError().getMsg()==null?"":node.getError().getMsg(), 1);
		}
		return e;
	}

	@Override
	public void afterReportNodeExecution(AbstractTimeBasedModel<ErrorRateEntry> model, ReportNode node) {
		ErrorRateEntry e = createPoint(node);
		if (e != null) {
			addPoint(model, node.getExecutionTime(), e);
		}
	}
	
	@Override
	protected void mergePoints(ErrorRateEntry target, ErrorRateEntry source) {
		target.count+=source.count;
		for(Entry<String, Integer> e:source.countByErrorMsg.entrySet()) {
			Integer count = target.countByErrorMsg.get(e.getKey());
			if(count==null) {
				count = e.getValue();
			} else {
				count = count+e.getValue();
			}
			target.countByErrorMsg.put(e.getKey(), count);
		}
	}

	@Override
	public String getViewId() {
		return "ErrorRate";
	}

	@Override
	public void rollbackReportNode(AbstractTimeBasedModel<ErrorRateEntry> model, ReportNode node) {
		ErrorRateEntry e = createPoint(node);
		if (e != null) {
			removePoint(model, node.getExecutionTime(), e);
		}
	}

	@Override
	protected void unMergePoints(ErrorRateEntry target, ErrorRateEntry source) {
		target.count-=source.count;
		for(Entry<String, Integer> e:source.countByErrorMsg.entrySet()) {
			Integer count = target.countByErrorMsg.get(e.getKey());
			if(count!=null) {
				count = count-e.getValue();
				target.countByErrorMsg.put(e.getKey(), count);
			}
			
		}
	}
}
