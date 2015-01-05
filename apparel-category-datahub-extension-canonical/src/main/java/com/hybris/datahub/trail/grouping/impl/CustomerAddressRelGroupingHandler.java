package com.hybris.datahub.trail.grouping.impl;

import java.util.ArrayList; 
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Preconditions;
import com.hybris.datahub.domain.CanonicalAttributeDefinition;
import com.hybris.datahub.domain.RawItem;
import com.hybris.datahub.grouping.GroupingHandler;
import com.hybris.datahub.model.CompositionGroup;
import com.hybris.datahub.model.DataItemAttribute;
import com.hybris.datahub.service.DataItemService;
import com.hybris.datahub.service.RawItemService;
import com.hybris.datahub.trail.domain.CanonicalCustomerAddressRel;

public class CustomerAddressRelGroupingHandler implements GroupingHandler {

	private DataItemService dataItemService;
	private RawItemService rawItemService;
	private static final String RAW_TYPE = "RawCustomer";
	private static final String BILL_TO = "billTo";
	private static final String SHIP_TO = "shipTo";
	private int order;

	public final DataItemService getDataItemService() {
		return dataItemService;
	}

	public final void setDataItemService(DataItemService dataItemService) {
		this.dataItemService = dataItemService;
	}

	public final RawItemService getRawItemService() {
		return rawItemService;
	}

	public final void setRawItemService(RawItemService rawItemService) {
		this.rawItemService = rawItemService;
	}

	public final void setOrder(int order) {
		this.order = order;
	}

	private Map<String, DataItemAttribute> getDataItemAttributeMap() {
		final Map<String, DataItemAttribute> attributeMap = new HashMap<String, DataItemAttribute>();
		attributeMap.put(BILL_TO,
				dataItemService.getDataItemAttribute(RAW_TYPE, BILL_TO));
		attributeMap.put(SHIP_TO,
				dataItemService.getDataItemAttribute(RAW_TYPE, SHIP_TO));
		return attributeMap;
	}

	private <T extends RawItem> List<T> removeAttributeValues(
			final List<T> items, final DataItemAttribute... attributesToRemove) {
		final List<T> newList = new ArrayList<T>();
		for (final T item : items) {
			final T customer = rawItemService.clone(item);
			for (final DataItemAttribute attr : attributesToRemove) {
				attr.setValue(customer, null);
			}
			newList.add(customer);
		}
		return newList;
	}

	private boolean groupHasPopulatedAttribute(
			final CompositionGroup<? extends RawItem> group,
			final DataItemAttribute attribute) {
		for (final RawItem item : group.getItems()) {
			if (StringUtils.isNotBlank((String) attribute.getValue(item))) {
				return true;
			}
		}
		return false;
	}

	public int getOrder() {
		return order;
	}

	public <T extends RawItem> List<CompositionGroup<T>> group(
			CompositionGroup<T> compositionGroup) {

		Preconditions.checkArgument(compositionGroup != null,
				"Composition Group cannot be null");
		assert compositionGroup != null;
		Preconditions.checkArgument(compositionGroup.getItems() != null,
				"Composition Group Items cannot be null");
		Preconditions.checkArgument(!compositionGroup.getItems().isEmpty(),
				"Composition Group must contain RawItems");
		Preconditions
				.checkArgument(
						compositionGroup.getItems().get(0) instanceof RawCustomer,
						"CustomerAddressRelGrouping Handler"
								+ " must have a composition group containing RawCustomers");
		final Map<String, DataItemAttribute> attributeMap = getDataItemAttributeMap();
		final List<CompositionGroup<T>> newGroups = new ArrayList<CompositionGroup<T>>();
		// create a group that does not have billTo or shipTo (integrationKey
		// only) for the default address
		newGroups.add(new CompositionGroup<T>(removeAttributeValues(
				compositionGroup.getItems(), attributeMap.get(BILL_TO),
				attributeMap.get(SHIP_TO)), compositionGroup.getAttributes(), null));
		
		/*newGroups.add(new CompositionGroup<T>(removeAttributeValues(
				compositionGroup.getItems(), attributeMap.get(BILL_TO),
				attributeMap.get(SHIP_TO)), compositionGroup.getAttributes()));
		*/
		if (groupHasPopulatedAttribute(compositionGroup,
				attributeMap.get(SHIP_TO))) {
			// create a group that does not have billTo to use for the shipTo
			// address creation
			newGroups.add(new CompositionGroup<T>(removeAttributeValues(
					compositionGroup.getItems(), attributeMap.get(BILL_TO)),
					compositionGroup.getAttributes(), null));
		}
		if (groupHasPopulatedAttribute(compositionGroup,
				attributeMap.get(BILL_TO))) {
			// create a group that does not have shipTo to use for the billTo
			// address creation
			newGroups.add(new CompositionGroup<T>(removeAttributeValues(
					compositionGroup.getItems(), attributeMap.get(SHIP_TO)),
					compositionGroup.getAttributes(), null));
		}
		return newGroups;
	}

	public <T extends RawItem> boolean isApplicable(
			CompositionGroup<T> compositionGroup) {
		Preconditions.checkArgument(compositionGroup != null,
				"Composition Group cannot be null");
		assert compositionGroup != null;
		Preconditions.checkArgument(compositionGroup.getItems() != null,
				"Composition Group Items cannot be null");
		Preconditions.checkArgument(!compositionGroup.getItems().isEmpty(),
				"Composition Group must contain RawItems");
		for (final DataItemAttribute attr : getDataItemAttributeMap().values()) {
			if (attr == null) {
				return false;
			}
		}
		if (!(compositionGroup.getItems().get(0) instanceof RawCustomer)) {
			return false;
		}
		for (final CanonicalAttributeDefinition attr : compositionGroup
				.getAttributes()) {
			if (!attr.getCanonicalAttributeModelDefinition()
					.getCanonicalItemMetadata().getItemType()
					.equals(CanonicalCustomerAddressRel._TYPECODE)) {
				return false;
			}
		}
		return true;
	}
}
