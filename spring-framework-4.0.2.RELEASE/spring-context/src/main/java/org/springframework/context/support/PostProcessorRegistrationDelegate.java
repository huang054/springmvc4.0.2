/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.context.support;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.DestructionAwareBeanPostProcessor;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.MergedBeanDefinitionPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.core.OrderComparator;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;

/**
 * Delegate for AbstractApplicationContext's post-processor handling.
 *
 * @author Juergen Hoeller
 * @since 4.0
 */
class PostProcessorRegistrationDelegate {
//可以看到BeanDefinitionRegistryPostProcessor中的回调是优先于BeanPostProcessor的。
//BeanDefinitionRegistryPostProcessor的作用是针对registry注册、修改BeanDefinition用的。
	public static void invokeBeanFactoryPostProcessors(
			ConfigurableListableBeanFactory beanFactory, List<BeanFactoryPostProcessor> beanFactoryPostProcessors) {

		// Invoke BeanDefinitionRegistryPostProcessors first, if any.
		Set<String> processedBeans = new HashSet<String>();

		if (beanFactory instanceof BeanDefinitionRegistry) {
			BeanDefinitionRegistry registry = (BeanDefinitionRegistry) beanFactory;
			List<BeanFactoryPostProcessor> regularPostProcessors = new LinkedList<BeanFactoryPostProcessor>();
			List<BeanDefinitionRegistryPostProcessor> registryPostProcessors =
					new LinkedList<BeanDefinitionRegistryPostProcessor>();
			//这里将通过应用上下文注册的BeanFacotryPostProcessor根据是否是它的子类型BeanDefinitionRegistryPostProcessor划分为两个部分
			for (BeanFactoryPostProcessor postProcessor : beanFactoryPostProcessors) {
				if (postProcessor instanceof BeanDefinitionRegistryPostProcessor) {
					//这里先执行BeanDefinitionRegistryPostProcessor拓展的postProcessBeanDefinitionRegistry回调。
					//由于它是先与postProcessBeanFactory回调进行调用的，因此通过postProcessBeanDefinitionRegistry中注册BeanFactoryPostProcessor的
					//话，是没问题的。

					BeanDefinitionRegistryPostProcessor registryPostProcessor =
							(BeanDefinitionRegistryPostProcessor) postProcessor;
					registryPostProcessor.postProcessBeanDefinitionRegistry(registry);
					registryPostProcessors.add(registryPostProcessor);
				}
				else {
					regularPostProcessors.add(postProcessor);
				}
			}

			// Do not initialize FactoryBeans here: We need to leave all regular beans
			// uninitialized to let the bean factory post-processors apply to them!
			// Separate between BeanDefinitionRegistryPostProcessors that implement
			// PriorityOrdered, Ordered, and the rest.
			// First, invoke the BeanDefinitionRegistryPostProcessors that implement PriorityOrdered.
			//由于上面在创建BeanFactory时进行过一次BeanDefinition的加载，因此在BeanFactory中会存在BeanDefinitionRegistryPostProcessor类型的BeanDefinition
			//这里仅仅是获取名称，并未进行实例化

			String[] postProcessorNames =
					beanFactory.getBeanNamesForType(BeanDefinitionRegistryPostProcessor.class, true, false);

			// First, invoke the BeanDefinitionRegistryPostProcessors that implement PriorityOrdered.
			//这里把实现了PriorityOrdered的Processor进行优先处理并进行排序
			List<BeanDefinitionRegistryPostProcessor> priorityOrderedPostProcessors = new ArrayList<BeanDefinitionRegistryPostProcessor>();
			for (String ppName : postProcessorNames) {
				//这里beanFactory中，由于实例尚未初始化，因此是通过BeanDefinition来判断类型是否匹配的
				if (beanFactory.isTypeMatch(ppName, PriorityOrdered.class)) {
					priorityOrderedPostProcessors.add(beanFactory.getBean(ppName, BeanDefinitionRegistryPostProcessor.class));
					processedBeans.add(ppName);
				}
			}
			//根据PriorityOrdered进行排序
			OrderComparator.sort(priorityOrderedPostProcessors);
			//未实现排序的追到到后面去
			registryPostProcessors.addAll(priorityOrderedPostProcessors);
			//这里先执行BeanDefinitionRegistryPostProcessor拓展的postProcessBeanDefinitionRegistry回调。
			invokeBeanDefinitionRegistryPostProcessors(priorityOrderedPostProcessors, registry);

			// Next, invoke the BeanDefinitionRegistryPostProcessors that implement Ordered.
			//这里处理实现Ordered的BeanDefinitionRegistryPostProcessors，由于PriorityOrdered先于
			postProcessorNames = beanFactory.getBeanNamesForType(BeanDefinitionRegistryPostProcessor.class, true, false);
			List<BeanDefinitionRegistryPostProcessor> orderedPostProcessors = new ArrayList<BeanDefinitionRegistryPostProcessor>();
			for (String ppName : postProcessorNames) {
				if (!processedBeans.contains(ppName) && beanFactory.isTypeMatch(ppName, Ordered.class)) {
					orderedPostProcessors.add(beanFactory.getBean(ppName, BeanDefinitionRegistryPostProcessor.class));
					processedBeans.add(ppName);
				}
			}
			OrderComparator.sort(orderedPostProcessors);
			registryPostProcessors.addAll(orderedPostProcessors);
			invokeBeanDefinitionRegistryPostProcessors(orderedPostProcessors, registry);

			// Finally, invoke all other BeanDefinitionRegistryPostProcessors until no further ones appear.
			//循环处理BeanDefinitionRegistryPostProcessors直到没有类型为BeanDefinitionRegistryPostProcessor的为止
			boolean reiterate = true;
			while (reiterate) {
				reiterate = false;
				postProcessorNames = beanFactory.getBeanNamesForType(BeanDefinitionRegistryPostProcessor.class, true, false);
				for (String ppName : postProcessorNames) {
					if (!processedBeans.contains(ppName)) {
						BeanDefinitionRegistryPostProcessor pp = beanFactory.getBean(ppName, BeanDefinitionRegistryPostProcessor.class);
						registryPostProcessors.add(pp);
						processedBeans.add(ppName);
						pp.postProcessBeanDefinitionRegistry(registry);
						reiterate = true;
					}
				}
			}

			// Now, invoke the postProcessBeanFactory callback of all processors handled so far.
			//调用BeanPostProcessor中的回调
			invokeBeanFactoryPostProcessors(registryPostProcessors, beanFactory);
			invokeBeanFactoryPostProcessors(regularPostProcessors, beanFactory);
		}

		else {
			// Invoke factory processors registered with the context instance.
			invokeBeanFactoryPostProcessors(beanFactoryPostProcessors, beanFactory);
		}

		// Do not initialize FactoryBeans here: We need to leave all regular beans
		// uninitialized to let the bean factory post-processors apply to them!
		//处理所有通过BeanDefinitionRegistryPostProcessor注册BeanPostProcessor
		String[] postProcessorNames =
				beanFactory.getBeanNamesForType(BeanFactoryPostProcessor.class, true, false);

		// Separate between BeanFactoryPostProcessors that implement PriorityOrdered,
		// Ordered, and the rest.
		//分组
		List<BeanFactoryPostProcessor> priorityOrderedPostProcessors = new ArrayList<BeanFactoryPostProcessor>();
		List<String> orderedPostProcessorNames = new ArrayList<String>();
		List<String> nonOrderedPostProcessorNames = new ArrayList<String>();
		for (String ppName : postProcessorNames) {
			if (processedBeans.contains(ppName)) {
				// skip - already processed in first phase above
			}
			else if (beanFactory.isTypeMatch(ppName, PriorityOrdered.class)) {
				priorityOrderedPostProcessors.add(beanFactory.getBean(ppName, BeanFactoryPostProcessor.class));
			}
			else if (beanFactory.isTypeMatch(ppName, Ordered.class)) {
				orderedPostProcessorNames.add(ppName);
			}
			else {
				nonOrderedPostProcessorNames.add(ppName);
			}
		}

		// First, invoke the BeanFactoryPostProcessors that implement PriorityOrdered.
		//排序调用
		OrderComparator.sort(priorityOrderedPostProcessors);
		invokeBeanFactoryPostProcessors(priorityOrderedPostProcessors, beanFactory);

		// Next, invoke the BeanFactoryPostProcessors that implement Ordered.
		List<BeanFactoryPostProcessor> orderedPostProcessors = new ArrayList<BeanFactoryPostProcessor>();
		for (String postProcessorName : orderedPostProcessorNames) {
			orderedPostProcessors.add(beanFactory.getBean(postProcessorName, BeanFactoryPostProcessor.class));
		}
		OrderComparator.sort(orderedPostProcessors);
		invokeBeanFactoryPostProcessors(orderedPostProcessors, beanFactory);

		// Finally, invoke all other BeanFactoryPostProcessors.
		List<BeanFactoryPostProcessor> nonOrderedPostProcessors = new ArrayList<BeanFactoryPostProcessor>();
		for (String postProcessorName : nonOrderedPostProcessorNames) {
			nonOrderedPostProcessors.add(beanFactory.getBean(postProcessorName, BeanFactoryPostProcessor.class));
		}
		invokeBeanFactoryPostProcessors(nonOrderedPostProcessors, beanFactory);
		//可以看到这里BeanFactoryPostProcessors的顺序细节
		//1.直接注册到AbstractApplicationContext中的beanFactoryPostProcessors且类型为BeanDefinitionRegistryPostProcessor
		//    执行BeanDefinitionRegistryPostProcessor.postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry)
		//2.被BeanFactory通过AbstractRefreshableApplicationContext创建BeanFactory时的loadBeanDefinition加载的（或者第一步修改registry注册进去的）类型为
		//    BeanDefinitionRegistryPostProcessor的而且实现了PriorityOrdered接口的BeanDefinition。然后根据getOrder()的值通过排序器
		//    ((DefaultListableBeanFactory) beanFactory).getDependencyComparator()如果不存在则用OrderComparator.INSTANCE。
		//    调用postProcessor.postProcessBeanDefinitionRegistry(registry);
		//3.被BeanFactory通过AbstractRefreshableApplicationContext创建BeanFactory时的loadBeanDefinition加载的（或者第一步、第二步修改registry注册进去的）类型为
		//    BeanDefinitionRegistryPostProcessor的而且实现了Ordered接口的BeanDefinition,然后根据getOrder()的值通过排序器
		//    ((DefaultListableBeanFactory) beanFactory).getDependencyComparator()如果不存在则用OrderComparator.INSTANCE。
		//    调用postProcessor.postProcessBeanDefinitionRegistry(registry);
		//    注意：PriorityOrdered是Ordered的子接口，因此这里就算第二部注册了一些实现了PriorityOrdered的BeanDefinitionRegistryPostProcessor，
		//    仅仅会根据getOrder()值进行排序执行的。
		//4.到这可能会由3步骤产生新的BeanDefinitionRegistryPostProcessor类型的BeanDefinition，而且每一次调用都可能会产生新的该类型BeanDefinition
		//    这里直接循环（+ 排序）调用postProcessor.postProcessBeanDefinitionRegistry(registry);，
		//    直至没有需要处理的BeanDefinitionRegistryPostProcessor类型的为止。
		//5.执行上面获取的所有BeanPostProcessor中的postProcessor.postProcessBeanFactory(beanFactory);
		//    虽然BeanDefinitionRegistryPostProcessor为BeanPostProcessor的子类，但是上面处理并没有开始执行postProcessor.postProcessBeanFactory(beanFactory);
		//    方法，这一步直接全部挨个先调用BeanDefinitionRegistryPostProcessor的postProcessBeanFactory(beanFactory)后调用
		//    上面从AbstractApplicationContext中获取的类型不为BeanDefinitionRegistryPostProcessor以及1、2、3、4获取的类型为BeanPostProcessor的
		//    postProcessBeanFactory(beanFactory)回调。
		//6.在第四步循环处理的是为子类型BeanDefinitionRegistryPostProcessor，因此会产生很多新的BeanPostProcessor这里统一处理有类型为BeanFactoryPostProcessor的
		//   将所有类型为BeanPostProcessor的根据实现了1、ProrityOrdered接口的与实现2、Ordered接口的以及3、未实现排序接口的分为三组
		//   然后排序依次执行

	}

	public static void registerBeanPostProcessors(
			ConfigurableListableBeanFactory beanFactory, AbstractApplicationContext applicationContext) {

		String[] postProcessorNames = beanFactory.getBeanNamesForType(BeanPostProcessor.class, true, false);

		// Register BeanPostProcessorChecker that logs an info message when
		// a bean is created during BeanPostProcessor instantiation, i.e. when
		// a bean is not eligible for getting processed by all BeanPostProcessors.
		//需要执行是BeanPostProcessor的个数 = 注册到BeanFactory中的个数 + BeanDefinition类型为BeanPostProcessor的个数 + 下面 1 个
		int beanProcessorTargetCount = beanFactory.getBeanPostProcessorCount() + 1 + postProcessorNames.length;
		beanFactory.addBeanPostProcessor(new BeanPostProcessorChecker(beanFactory, beanProcessorTargetCount));

		// Separate between BeanPostProcessors that implement PriorityOrdered,
		// Ordered, and the rest.
		//根据PriorityOrdered、Ordered、其他进行分组
		List<BeanPostProcessor> priorityOrderedPostProcessors = new ArrayList<BeanPostProcessor>();
		List<BeanPostProcessor> internalPostProcessors = new ArrayList<BeanPostProcessor>();
		List<String> orderedPostProcessorNames = new ArrayList<String>();
		List<String> nonOrderedPostProcessorNames = new ArrayList<String>();
		for (String ppName : postProcessorNames) {
			if (beanFactory.isTypeMatch(ppName, PriorityOrdered.class)) {
				BeanPostProcessor pp = beanFactory.getBean(ppName, BeanPostProcessor.class);
				priorityOrderedPostProcessors.add(pp);
				if (pp instanceof MergedBeanDefinitionPostProcessor) {
					internalPostProcessors.add(pp);
				}
			}
			else if (beanFactory.isTypeMatch(ppName, Ordered.class)) {
				orderedPostProcessorNames.add(ppName);
			}
			else {
				nonOrderedPostProcessorNames.add(ppName);
			}
		}

		// First, register the BeanPostProcessors that implement PriorityOrdered.
		//priorityOrdered组排序注册到BeanFactory
		OrderComparator.sort(priorityOrderedPostProcessors);
		registerBeanPostProcessors(beanFactory, priorityOrderedPostProcessors);

		// Next, register the BeanPostProcessors that implement Ordered.
		//ordered组排序注册到BeanFactory
		List<BeanPostProcessor> orderedPostProcessors = new ArrayList<BeanPostProcessor>();
		for (String ppName : orderedPostProcessorNames) {
			BeanPostProcessor pp = beanFactory.getBean(ppName, BeanPostProcessor.class);
			orderedPostProcessors.add(pp);
			if (pp instanceof MergedBeanDefinitionPostProcessor) {
				internalPostProcessors.add(pp);
			}
		}
		OrderComparator.sort(orderedPostProcessors);
		registerBeanPostProcessors(beanFactory, orderedPostProcessors);

		// Now, register all regular BeanPostProcessors.
		//普通的BeanPostProcessor注册到BeanFactory
		List<BeanPostProcessor> nonOrderedPostProcessors = new ArrayList<BeanPostProcessor>();
		for (String ppName : nonOrderedPostProcessorNames) {
			BeanPostProcessor pp = beanFactory.getBean(ppName, BeanPostProcessor.class);
			nonOrderedPostProcessors.add(pp);
			if (pp instanceof MergedBeanDefinitionPostProcessor) {
				internalPostProcessors.add(pp);
			}
		}
		registerBeanPostProcessors(beanFactory, nonOrderedPostProcessors);

		// Finally, re-register all internal BeanPostProcessors.
		//将MergedBeanDefinitionPostProcessor类型的重新注册到靠后的位置
		OrderComparator.sort(internalPostProcessors);
		registerBeanPostProcessors(beanFactory, internalPostProcessors);
		//将ApplicationListenerDetector这个processor放到最后面
		//在前面prepareBeanFactory(ConfigurableListableBeanFactory beanFactory)方法中注册过一次，是为了处理BeanFactoryPostProcessor处理过程中可能存在的注册
		//这里重新注册下是为了将这个BeanPostProcessor放到最最后面处理，因为如果可能存在一些通过代理实现监听的对象，包保证它一定能处理到。

		beanFactory.addBeanPostProcessor(new ApplicationListenerDetector(applicationContext));
		//BeanPostProcessor注册在BeanFacotry中的顺序关系
		//1.此前直接注册到BeanFactory中的顺序不变
		//2.BeanPostProcessorChecker紧随此前直接注册到BeanFacotry中BeanPostProcessor
		//3.实现了PriorityOrdered接口的BeanPostProcessor (根据getOrder()排序)
		//4.实现了Ordered接口的BeanPostProcessor（根据getOrder()排序）
		//5.未实现关于Order接口的普通BeanPostProcessor
		//6.所有类型为BeanPostProcessor的子类型的MergedBeanDefinitionPostProcessor重新注册，使它们的执行顺序在BeanPostProcessor直接类型的后面
		//7.ApplicationListenerDetector这个BeanPostProcessor放到最最后面执行

	}

	/**
	 * Invoke the given BeanDefinitionRegistryPostProcessor beans.
	 */
	private static void invokeBeanDefinitionRegistryPostProcessors(
			Collection<? extends BeanDefinitionRegistryPostProcessor> postProcessors, BeanDefinitionRegistry registry) {

		for (BeanDefinitionRegistryPostProcessor postProcessor : postProcessors) {
			postProcessor.postProcessBeanDefinitionRegistry(registry);
		}
	}

	/**
	 * Invoke the given BeanFactoryPostProcessor beans.
	 */
	private static void invokeBeanFactoryPostProcessors(
			Collection<? extends BeanFactoryPostProcessor> postProcessors, ConfigurableListableBeanFactory beanFactory) {

		for (BeanFactoryPostProcessor postProcessor : postProcessors) {
			postProcessor.postProcessBeanFactory(beanFactory);
		}
	}

	/**
	 * Register the given BeanPostProcessor beans.
	 */
	private static void registerBeanPostProcessors(
			ConfigurableListableBeanFactory beanFactory, List<BeanPostProcessor> postProcessors) {

		for (BeanPostProcessor postProcessor : postProcessors) {
			beanFactory.addBeanPostProcessor(postProcessor);
		}
	}


	/**
	 * BeanPostProcessor that logs an info message when a bean is created during
	 * BeanPostProcessor instantiation, i.e. when a bean is not eligible for
	 * getting processed by all BeanPostProcessors.
	 */
	private static class BeanPostProcessorChecker implements BeanPostProcessor {

		private static final Log logger = LogFactory.getLog(BeanPostProcessorChecker.class);

		private final ConfigurableListableBeanFactory beanFactory;

		private final int beanPostProcessorTargetCount;

		public BeanPostProcessorChecker(ConfigurableListableBeanFactory beanFactory, int beanPostProcessorTargetCount) {
			this.beanFactory = beanFactory;
			this.beanPostProcessorTargetCount = beanPostProcessorTargetCount;
		}

		@Override
		public Object postProcessBeforeInitialization(Object bean, String beanName) {
			return bean;
		}

		@Override
		public Object postProcessAfterInitialization(Object bean, String beanName) {
			if (bean != null && !(bean instanceof BeanPostProcessor) &&
					this.beanFactory.getBeanPostProcessorCount() < this.beanPostProcessorTargetCount) {
				if (logger.isInfoEnabled()) {
					logger.info("Bean '" + beanName + "' of type [" + bean.getClass() +
							"] is not eligible for getting processed by all BeanPostProcessors " +
							"(for example: not eligible for auto-proxying)");
				}
			}
			return bean;
		}
	}


	/**
	 * BeanPostProcessor that detects beans which implement the ApplicationListener interface.
	 * This catches beans that can't reliably be detected by getBeanNamesForType.
	 */
	private static class ApplicationListenerDetector implements MergedBeanDefinitionPostProcessor, DestructionAwareBeanPostProcessor {

		private static final Log logger = LogFactory.getLog(ApplicationListenerDetector.class);

		private final AbstractApplicationContext applicationContext;

		private final Map<String, Boolean> singletonNames = new ConcurrentHashMap<String, Boolean>(64);

		public ApplicationListenerDetector(AbstractApplicationContext applicationContext) {
			this.applicationContext = applicationContext;
		}

		@Override
		public void postProcessMergedBeanDefinition(RootBeanDefinition beanDefinition, Class<?> beanType, String beanName) {
			if (beanDefinition.isSingleton()) {
				this.singletonNames.put(beanName, Boolean.TRUE);
			}
		}

		@Override
		public Object postProcessBeforeInitialization(Object bean, String beanName) {
			return bean;
		}

		@Override
		public Object postProcessAfterInitialization(Object bean, String beanName) {
			if (bean instanceof ApplicationListener) {
				// potentially not detected as a listener by getBeanNamesForType retrieval
				Boolean flag = this.singletonNames.get(beanName);
				if (Boolean.TRUE.equals(flag)) {
					// singleton bean (top-level or inner): register on the fly
					this.applicationContext.addApplicationListener((ApplicationListener<?>) bean);
				}
				else if (flag == null) {
					if (logger.isWarnEnabled() && !this.applicationContext.containsBean(beanName)) {
						// inner bean with other scope - can't reliably process events
						logger.warn("Inner bean '" + beanName + "' implements ApplicationListener interface " +
								"but is not reachable for event multicasting by its containing ApplicationContext " +
								"because it does not have singleton scope. Only top-level listener beans are allowed " +
								"to be of non-singleton scope.");
					}
					this.singletonNames.put(beanName, Boolean.FALSE);
				}
			}
			return bean;
		}

		@Override
		public void postProcessBeforeDestruction(Object bean, String beanName) {
			if (bean instanceof ApplicationListener) {
				ApplicationEventMulticaster multicaster = this.applicationContext.getApplicationEventMulticaster();
				multicaster.removeApplicationListener((ApplicationListener<?>) bean);
				multicaster.removeApplicationListenerBean(beanName);
			}
		}
	}

}
