package com.company.treelogic.view.metafield;

import com.company.treelogic.entity.MetaField;
import com.company.treelogic.view.main.MainView;
import com.vaadin.flow.router.Route;
import io.jmix.flowui.view.EditedEntityContainer;
import io.jmix.flowui.view.StandardDetailView;
import io.jmix.flowui.view.ViewController;
import io.jmix.flowui.view.ViewDescriptor;

@Route(value = "meta-fields/:id", layout = MainView.class)
@ViewController(id = "MetaField.detail")
@ViewDescriptor(path = "meta-field-detail-view.xml")
@EditedEntityContainer("metaFieldDc")
public class MetaFieldDetailView extends StandardDetailView<MetaField> {
}