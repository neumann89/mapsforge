package org.mapsforge.storage.poi;

/**
 * This helper class provides templates for category configurations.
 * 
 * @author Karsten Groll
 */
public class CategoryTreeBuilder {
	static PoiCategory createAndGetBalancedConfiguration() {
		// Level 0
		PoiCategory root = new DoubleLinkedPoiCategory("root", null);

		// Level 1
		PoiCategory l1_1 = new DoubleLinkedPoiCategory("l1_1", root);
		PoiCategory l1_2 = new DoubleLinkedPoiCategory("l1_2", root);
		PoiCategory l1_3 = new DoubleLinkedPoiCategory("l1_3", root);

		// Level 2
		new DoubleLinkedPoiCategory("l1_1_l2_1", l1_1);
		new DoubleLinkedPoiCategory("l1_1_l2_2", l1_1);
		new DoubleLinkedPoiCategory("l1_1_l2_3", l1_1);

		new DoubleLinkedPoiCategory("l1_2_l2_1", l1_2);
		new DoubleLinkedPoiCategory("l1_2_l2_2", l1_2);
		new DoubleLinkedPoiCategory("l1_2_l2_3", l1_2);

		new DoubleLinkedPoiCategory("l1_3_l2_1", l1_3);
		new DoubleLinkedPoiCategory("l1_3_l2_2", l1_3);
		new DoubleLinkedPoiCategory("l1_3_l2_3", l1_3);

		DoubleLinkedPoiCategory.calculateCategoryIDs((DoubleLinkedPoiCategory) root, 0);
		return root;
	}

	static PoiCategory createAndGetFlatConfiguration() {
		PoiCategory root = new DoubleLinkedPoiCategory("root", null);
		new DoubleLinkedPoiCategory("a", root);
		new DoubleLinkedPoiCategory("b", root);
		new DoubleLinkedPoiCategory("c", root);
		new DoubleLinkedPoiCategory("d", root);
		new DoubleLinkedPoiCategory("e", root);

		DoubleLinkedPoiCategory.calculateCategoryIDs((DoubleLinkedPoiCategory) root, 0);
		return root;
	}
}
