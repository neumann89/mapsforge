package org.mapsforge.storage.poi;

import java.util.Collection;

import junit.framework.Assert;

import org.junit.Test;

/**
 * Tests the {@link WhitelistPoiCategoryFilter}.
 * 
 * @author Karsten Groll
 * 
 */
public class WhiteListCategoryFilterTest {
	@Test
	public void testAcceptedCategoriesForFlatHierarchy()
			throws UnknownPoiCategoryException {

		// ACCEPT ALL NODES
		PoiCategory root = CategoryTreeBuilder.createAndGetFlatConfiguration();
		DoubleLinkedPoiCategory.calculateCategoryIDs(
				(DoubleLinkedPoiCategory) root, 0);
		PoiCategoryFilter filter = new WhitelistPoiCategoryFilter();
		PoiCategoryManager cm = new CategoryManagerTest.MockPoiCategoryManager(
				root);

		filter.addCategory(root);
		Collection<PoiCategory> acceptedCategories = filter
				.getAcceptedCategories();
		Collection<PoiCategory> acceptedSuperCategories = filter
				.getAcceptedSuperCategories();

		// There should be 1+5 = 6 accepted categories
		Assert.assertEquals(6, acceptedCategories.size());

		// ACCEPT ONE CHILD NODE ONLY
		filter = new WhitelistPoiCategoryFilter();
		filter.addCategory(cm.getPoiCategoryByTitle("a"));
		acceptedCategories = filter.getAcceptedCategories();
		acceptedSuperCategories = filter.getAcceptedSuperCategories();

		// Only one category should be accepted
		Assert.assertEquals(1, acceptedCategories.size());

		// The accepted category should have title "a" and ID 0
		PoiCategory categoryA = null;
		for (PoiCategory c : acceptedCategories) {
			if (c.getTitle() == "a") {
				categoryA = c;
			}
		}
		Assert.assertEquals("a", categoryA.getTitle());
		Assert.assertEquals(0, categoryA.getID());

		// There should be one super category now (a)
		Assert.assertEquals(1, acceptedCategories.size());
		Assert.assertTrue(acceptedCategories.contains(categoryA));

		// ACCEPT TWO CHILDREN
		filter.addCategory(cm.getPoiCategoryByTitle("d"));
		acceptedCategories = filter.getAcceptedCategories();
		acceptedSuperCategories = filter.getAcceptedSuperCategories();

		// There should be two accepted categories
		Assert.assertEquals(2, acceptedCategories.size());

		// The accepted categories should have title "a" and "d" and ID 0 and 3
		PoiCategory categoryD = null;
		for (PoiCategory c : acceptedCategories) {
			if (c.getTitle() == "d") {
				categoryD = c;
			}
		}
		Assert.assertEquals("a", categoryA.getTitle());
		Assert.assertEquals(0, categoryA.getID());
		Assert.assertEquals("d", categoryD.getTitle());
		Assert.assertEquals(3, categoryD.getID());

		// There should be two super categories now (a,d)
		Assert.assertEquals(2, acceptedCategories.size());
		Assert.assertTrue(acceptedCategories.contains(categoryA));
		Assert.assertTrue(acceptedCategories.contains(categoryD));

		// ACCEPT ALL NOW
		filter.addCategory(root);
		acceptedCategories = filter.getAcceptedCategories();
		acceptedSuperCategories = filter.getAcceptedSuperCategories();

		// There should now be 6 accepted categories
		Assert.assertEquals(6, acceptedCategories.size());

		// There should only be one super category left now (root)
		Assert.assertEquals(1, acceptedSuperCategories.size());
		Assert.assertTrue(acceptedSuperCategories.contains(root));

	}
}
