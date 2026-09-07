package com.github.leopoko.solclassic.utils;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public final class FoodHistory{
	public final Map<Item, Integer> amountConsumed;
	public final LinkedList<ItemStack> consumedItems;
	public void clear(){
		amountConsumed.clear();
		consumedItems.clear();
	}

	public FoodHistory() {
		amountConsumed = new HashMap<>();
		consumedItems = new LinkedList<>();
	}
	public void add(ItemStack item, int maxHistory){
		if(consumedItems.size() >= maxHistory) decrementConsumption(consumedItems.remove());
		consumedItems.add(item);
		incrementConsumption(item);
	}
	public void add(ItemStack item){
		consumedItems.add(item);
		incrementConsumption(item);
	}
	public int getAmountConsumed(ItemStack target){
		return amountConsumed.getOrDefault(target.getItem(), 0);
	}
	private int incrementConsumption(ItemStack item){
		int previousAmountConsumed = amountConsumed.getOrDefault(item.getItem(), 0);
		amountConsumed.put(item.getItem(), previousAmountConsumed + 1);
		return previousAmountConsumed + 1;
	}
	private int decrementConsumption(ItemStack item){
		if(!amountConsumed.containsKey(item.getItem())){
			Logger logger = Logger.getLogger("SOLClassic");
			logger.severe("About to throw, Printing class that is throwing error");
			logger.severe(toString());
			throw new IllegalStateException("Attempted to remove non-existent item \""+BuiltInRegistries.ITEM.getKey(item.getItem()).toString()+"\" from food history");
		}
		int previousAmountConsumed = amountConsumed.get(item.getItem());
		if(previousAmountConsumed <= 1){
			amountConsumed.remove(item.getItem());
			return 0;
		}
		else{
			amountConsumed.put(item.getItem(), previousAmountConsumed - 1);
			return previousAmountConsumed - 1;
		}
	}

	@Override
	public String toString() {
		Iterator<String> mapEntries = amountConsumed.entrySet().stream().map(pair -> "\""+ BuiltInRegistries.ITEM.getKey(pair.getKey()) +"\": "+ pair.getValue()).iterator();
		AtomicReference<String> mapValue = new AtomicReference<>("");
		if(mapEntries.hasNext()) {
			mapValue.set(mapEntries.next());
			mapEntries.forEachRemaining(entry -> mapValue.set(mapValue + ", " + entry));
		}
		return "FoodHistory:{Map: {%b}, Queue: %b}".formatted(mapValue.get(), consumedItems);
	}
}
