package me.Ccamm.XWeatherPlus.Weather.World.Types;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Cat;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;

import me.Ccamm.XWeatherPlus.Weather.WeatherHandler;
import me.Ccamm.XWeatherPlus.Weather.World.WorldWeather;

public class CatsAndDogs extends WorldWeather
{
	private static CatsAndDogs catsanddogs = null;
	private static HashSet<LivingEntity> toberemoved = new HashSet<LivingEntity>();
	private static double heightdif = 25;
	private static double spawnaboveplayer = 0.3;
	private static int playerradius;
	private static double chanceofcat;
	private static int particlecount;

	private CatsAndDogs(FileConfiguration config) {
		super("CatsAndDogs", config, 10);
		WorldWeather.addWeatherType(this);
	}

	public static CatsAndDogs setUpCatsAndDogs(FileConfiguration config)
	{
		if(catsanddogs == null) {
			catsanddogs = new CatsAndDogs(config);
		} else {
			catsanddogs.loadConfig(config);
			catsanddogs.reloadName();
		}
		return catsanddogs;
	}

	@Override
	public void loadMoreOptions(FileConfiguration config)
	{
		playerradius = config.getInt("CatsAndDogs.RadiusAroundPlayer");
		particlecount = config.getInt("CatsAndDogs.AnimalCount");
		chanceofcat = config.getDouble("CatsAndDogs.CatChance");
	}

	@Override
	protected void startWeather(World w, int dur)
	{
		WeatherHandler.setRain(w, dur);
	}

	@Override
	protected void weatherEffect(World w)
	{
		for(Player p : w.getPlayers()) {
			if(WeatherHandler.locationIsProtected(p.getLocation())
					|| WeatherHandler.isDryBiome(p.getLocation())) {continue;}
			spawnAnimals(p);
		}
		removeStillAnimals();
	}

	private void removeStillAnimals()
	{
		List<LivingEntity> remove = new ArrayList<LivingEntity>();
		Location loc;
		for(LivingEntity le : toberemoved) {
			if(le.isDead()) {
				remove.add(le);
			}
			loc = le.getLocation();
			if(loc.getBlockY() <= WeatherHandler.getHighestY(loc)+1) {
				if(le.getType().equals(EntityType.CAT)) {
					loc.getWorld().playSound(loc, Sound.ENTITY_CAT_AMBIENT, SoundCategory.WEATHER, (float) 0.2, 0);
				} else {
					loc.getWorld().playSound(loc, Sound.ENTITY_WOLF_AMBIENT, SoundCategory.WEATHER, (float) 0.2, 0);
				}
				remove.add(le);
				le.remove();
			}
		}

		for(LivingEntity le : remove) {
			toberemoved.remove(le);
		}
	}

	private void removeAllAnimals(World w)
	{
		List<LivingEntity> remove = new ArrayList<LivingEntity>();
		for(LivingEntity le : toberemoved) {
			if(le.getWorld().equals(w)) {
				remove.add(le);
			}
		}

		for(LivingEntity le : remove) {
			toberemoved.remove(le);
			le.remove();
		}
	}

	@Override
	protected void endWeather(World world)
	{
		removeAllAnimals(world);
		if(rainafter) {
			WeatherHandler.setRain(world, duration);
		} else {
			WeatherHandler.setSunny(world, duration);
		}
	}

	private void spawnAnimals(Player p)
	{
		Random r = new Random();
		Location loc = p.getLocation().clone();
		double dx;
		double dz;
		int n = 0;

		if(r.nextDouble() <= spawnaboveplayer) {
			n++;
			dx = 1.5*r.nextDouble()*(r.nextBoolean() ? 1:-1);
			dz = 1.5*r.nextDouble()*(r.nextBoolean() ? 1:-1);
			loc.add(dx, 0, dz);
			if(WeatherHandler.isLocationLoaded(loc)
					&& !WeatherHandler.isDryBiome(p.getLocation())
					&& !WeatherHandler.locationIsProtected(loc)) {
				if(r.nextDouble() <= chanceofcat) {
					loc.setY(p.getLocation().getY() + 90);
					loc.subtract(0, (r.nextBoolean() ? 1:-1)*heightdif*r.nextDouble(), 0);
					if(loc.getY() > loc.getWorld().getMaxHeight()) {
						loc.setY(loc.getWorld().getMaxHeight());
					}
					spawnCat(loc, r);
				} else {
					loc.setY(p.getWorld().getMaxHeight());
					loc.subtract(0, heightdif*r.nextDouble(), 0);
					spawnDog(loc);
				}
			}
		}

		for(int i = 0; i < particlecount-n; i++) {
			dx = playerradius*r.nextDouble()*(r.nextBoolean() ? 1:-1);
			dz = playerradius*r.nextDouble()*(r.nextBoolean() ? 1:-1);
			loc.add(dx, 0, dz);
			if(!WeatherHandler.isLocationLoaded(loc)
					|| WeatherHandler.isDryBiome(p.getLocation())
					|| WeatherHandler.locationIsProtected(loc)) {
				continue;
			}
			loc.setY(p.getWorld().getMaxHeight());
			loc.subtract(0, heightdif*r.nextDouble(), 0);

			if(r.nextDouble() <= chanceofcat) {
				loc.setY(p.getLocation().getY() + 90);
				loc.subtract(0, (r.nextBoolean() ? 1:-1)*heightdif*r.nextDouble(), 0);
				if(loc.getY() > loc.getWorld().getMaxHeight()) {
					loc.setY(loc.getWorld().getMaxHeight());
				}
				spawnCat(loc, r);
			} else {
				spawnDog(loc);
			}
		}
	}

	// Let's try not make  stuff more complicated than it should be
	@Deprecated(forRemoval = true)
	private void spawnCat(Location loc, Random r)
	{
		Cat cat = (Cat) loc.getWorld().spawnEntity(loc, EntityType.CAT);
		Cat.Type[] cattypes = Cat.Type.values();
		cat.setCatType(cattypes[r.nextInt(cattypes.length)]);
		cat.setInvulnerable(true);
		toberemoved.add((LivingEntity) cat);
	}

	private void spawnDog(Location loc)
	{
		Wolf dog = (Wolf) loc.getWorld().spawnEntity(loc, EntityType.WOLF);
		dog.setInvulnerable(true);
		toberemoved.add((LivingEntity) dog);
	}
}
