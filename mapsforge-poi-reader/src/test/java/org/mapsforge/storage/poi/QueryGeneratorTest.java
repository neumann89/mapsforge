package org.mapsforge.storage.poi;

import org.junit.Before;
import org.junit.Test;

/**
 * This class tests the {@link PoiCategoryRangeQueryGenerator} class for common use cases.
 * 
 * @author Karsten Groll
 */
public class QueryGeneratorTest {
	private PoiCategory flatRoot;
	private PoiCategory balancedRoot;
	private PoiCategoryManager flatCm;
	private PoiCategoryManager balancedCm;

	@Before
	public void init() {
		this.flatRoot = CategoryTreeBuilder.createAndGetFlatConfiguration();
		this.flatCm = new CategoryManagerTest.MockPoiCategoryManager(this.flatRoot);

		this.balancedRoot = CategoryTreeBuilder.createAndGetBalancedConfiguration();
		this.balancedCm = new CategoryManagerTest.MockPoiCategoryManager(this.balancedRoot);

		System.out.println("=====8<=====");
		System.out.println(DoubleLinkedPoiCategory.getGraphVizString((DoubleLinkedPoiCategory) this.balancedRoot));
		System.out.println("============");
	}

	/**
	 * Select all categories by adding the root category to a whitelist filter.
	 */
	// @Test
	public void selectAllFromFlatHierarchy() {
		PoiCategoryFilter filter = new WhitelistPoiCategoryFilter();
		filter.addCategory(this.flatRoot);

		String query = PoiCategoryRangeQueryGenerator.getSQLSelectString(filter);

		System.out.println("Query: " + query);

		// TODO add assertions
	}

	/**
	 * Select all categories by adding the root category to a whitelist filter.
	 * 
	 * @throws UnknownPoiCategoryException
	 *             if a category cannot be found by its name or ID.
	 */
	@Test
	public void selectTwoFromBalancedHierarchy() throws UnknownPoiCategoryException {
		PoiCategoryFilter filter = new WhitelistPoiCategoryFilter();
		filter.addCategory(this.balancedCm.getPoiCategoryByTitle("l1_1"));
		filter.addCategory(this.balancedCm.getPoiCategoryByTitle("l1_2"));

		String query = PoiCategoryRangeQueryGenerator.getSQLSelectString(filter);
		System.out.println("Query: " + query);

		// TODO add assertions
	}

}
