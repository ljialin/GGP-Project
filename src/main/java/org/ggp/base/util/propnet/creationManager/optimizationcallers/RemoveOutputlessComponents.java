package org.ggp.base.util.propnet.creationManager.optimizationcallers;

import org.ggp.base.util.propnet.architecture.separateExtendedState.dynamic.DynamicPropNet;
import org.ggp.base.util.propnet.factory.DynamicPropNetFactory;

public class RemoveOutputlessComponents implements OptimizationCaller {

	@Override
	public void optimize(DynamicPropNet dynamicPropNet)
			throws InterruptedException {

		DynamicPropNetFactory.removeOutputlessComponents(dynamicPropNet);

	}

}
