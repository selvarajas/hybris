package com.hybris.datahub.trail.composition.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Preconditions;
import com.hybris.datahub.composition.CompositionRuleHandler;
import com.hybris.datahub.composition.impl.AbstractCompositionRuleHandler;
import com.hybris.datahub.domain.CanonicalAttributeDefinition;
import com.hybris.datahub.domain.CanonicalItem;
import com.hybris.datahub.domain.RawItem;
import com.hybris.datahub.model.CompositionGroup;
import com.hybris.datahub.model.DataItemAttribute;

public class CustomerAddressRelCompositionHandler extends
		AbstractCompositionRuleHandler implements CompositionRuleHandler {
	private static final String CANONICAL_TYPE = "CanonicalCustomerAddressRel";
	private static final String CANONICAL_ADDRESS_TYPE = "addressType";
	private static final String CANONICAL_ADDRESS_ID = "addressId";
	private static final String RAW_TYPE = "RawCustomer";
	private static final String RAW_INTEGRATION_KEY = "integrationKey";
	private static final String RAW_BILL_TO = "billTo";
	private static final String RAW_SHIP_TO = "shipTo";

	public <T extends CanonicalItem> T compose(
			final CanonicalAttributeDefinition attribute,
			final CompositionGroup<? extends RawItem> compositionGroup,
			final T canonicalItem) {
		Preconditions.checkArgument(compositionGroup != null,
				"Composition Group cannot be null");
		assert compositionGroup != null;
		Preconditions.checkArgument(compositionGroup.getItems() != null,
				"Composition Group Items cannot be null");
		Preconditions.checkArgument(canonicalItem != null,
				"Canonical Item cannot be null");
		final Map<String, DataItemAttribute> rawAttributeMap = loadRawDataItemAttributes();
		final Map<String, DataItemAttribute> canonicalAttributeMap = loadCanonicalDataItemAttributes();
		// these fields are used to create the combined canonical integration
		// key
		final String customerId = findFirstPopulatedValue(
				compositionGroup.getItems(),
				rawAttributeMap.get(RAW_INTEGRATION_KEY));
		String addressId;
		addressId = findFirstPopulatedValue(compositionGroup.getItems(),
				rawAttributeMap.get(RAW_BILL_TO));
		if (addressId != null) {
			// this is a bill to address
			canonicalAttributeMap.get(CANONICAL_ADDRESS_ID).setValue(
					canonicalItem, addressId);
			canonicalAttributeMap.get(CANONICAL_ADDRESS_TYPE).setValue(
					canonicalItem, "Home Address");
			// AddressType.BILL_TO.toString()
		} else {
			addressId = findFirstPopulatedValue(compositionGroup.getItems(),
					rawAttributeMap.get(RAW_SHIP_TO));
			if (addressId != null) {
				// this is a ship to address
				canonicalAttributeMap.get(CANONICAL_ADDRESS_ID).setValue(
						canonicalItem, addressId);
				canonicalAttributeMap.get(CANONICAL_ADDRESS_TYPE).setValue(
						canonicalItem, "Home Address");// AddressType.BILL_TO.toString()
			}
		}
		if (addressId == null) {
			// this is a default address
			addressId = customerId;
			canonicalAttributeMap.get(CANONICAL_ADDRESS_ID).setValue(
					canonicalItem, addressId);
			canonicalAttributeMap.get(CANONICAL_ADDRESS_TYPE).setValue(
					canonicalItem, "Home Address");// AddressType.BILL_TO.toString()
		}
		return canonicalItem;
	}

	private String findFirstPopulatedValue(
			final List<? extends RawItem> rawItemList,
			final DataItemAttribute attribute) {
		for (final RawItem customer : rawItemList) {
			final String value = (String) attribute.getValue(customer);
			if (StringUtils.isNotBlank(value)) {
				return value;
			}
		}
		return null;
	}

	private Map<String, DataItemAttribute> loadRawDataItemAttributes() {
		final Map<String, DataItemAttribute> attributeMap = new HashMap<String, DataItemAttribute>();
		attributeMap.put(RAW_BILL_TO,
				getDataItemAttribute(RAW_TYPE, RAW_BILL_TO));
		attributeMap.put(RAW_SHIP_TO,
				getDataItemAttribute(RAW_TYPE, RAW_SHIP_TO));
		attributeMap.put(RAW_INTEGRATION_KEY,
				getDataItemAttribute(RAW_TYPE, RAW_INTEGRATION_KEY));
		validateDataItemAttributesExist(attributeMap);
		return attributeMap;
	}

	private Map<String, DataItemAttribute> loadCanonicalDataItemAttributes() {
		final Map<String, DataItemAttribute> attributeMap = new HashMap<String, DataItemAttribute>();
		attributeMap.put(CANONICAL_ADDRESS_ID,
				getDataItemAttribute(CANONICAL_TYPE, CANONICAL_ADDRESS_ID));
		attributeMap.put(CANONICAL_ADDRESS_TYPE,
				getDataItemAttribute(CANONICAL_TYPE, CANONICAL_ADDRESS_TYPE));
		validateDataItemAttributesExist(attributeMap);
		return attributeMap;
	}

	private void validateDataItemAttributesExist(
			final Map<String, DataItemAttribute> attrMap) {
		for (final Map.Entry<String, DataItemAttribute> attr : attrMap
				.entrySet()) {
			if (attr.getValue() == null) {
				throw new IllegalStateException(
						attr.getKey()
								+ " does not exist, cannot compose CanonicalCustomerAddressRel");
			}
		}
	}

	public boolean isApplicable(final CanonicalAttributeDefinition attribute) {
		return attribute.getCanonicalAttributeModelDefinition()
				.getAttributeName().equals(CANONICAL_ADDRESS_TYPE)
				&& attribute.getCanonicalAttributeModelDefinition()
						.getCanonicalItemMetadata().getItemType()
						.equals(CANONICAL_TYPE);
	}
}
