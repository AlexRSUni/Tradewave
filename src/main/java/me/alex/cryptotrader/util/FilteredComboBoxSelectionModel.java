package me.alex.cryptotrader.util;

import javafx.collections.transformation.FilteredList;
import javafx.scene.control.SingleSelectionModel;

public class FilteredComboBoxSelectionModel<T> extends SingleSelectionModel<T> {

    private final FilteredList<T> filteredItems;

    public FilteredComboBoxSelectionModel(FilteredList<T> filteredItems) {
        this.filteredItems = filteredItems;
    }

    @Override
    protected T getModelItem(int index) {
        return filteredItems.getSource().get(index);
    }

    @Override
    protected int getItemCount() {
        return filteredItems.getSource().size();
    }

    @Override
    public void select(int index) {
        if (index == -1) return;
        int sourceIndex = filteredItems.getSourceIndex(index);
        super.select(sourceIndex);
    }

    @Override
    public void clearAndSelect(int index) {
        if (index == -1) return;
        int sourceIndex = filteredItems.getSourceIndex(index);
        super.clearAndSelect(sourceIndex);
    }
}
