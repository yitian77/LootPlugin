package com.example.lootplugin;

import org.bukkit.inventory.ItemStack;

public class LootItem {
    private final ItemStack itemStack;
    private int maxAmount;
    private double probability;

    public LootItem(ItemStack itemStack, int maxAmount, double probability) {
        this.itemStack = itemStack;
        this.maxAmount = maxAmount;
        this.probability = probability;
    }

    public ItemStack getItemStack() {
        return this.itemStack;
    }

    public int getMaxAmount() {
        return this.maxAmount;
    }

    public void setMaxAmount(int maxAmount) {
        this.maxAmount = maxAmount;
    }

    public double getProbability() {
        return this.probability;
    }

    public void setProbability(double probability) {
        this.probability = probability;
    }
}