var Vector = Java.type("org.bukkit.util.Vector");

/**
 * 方法名称和foods.yml中的键名称相同
 * 例如本例中的mole将在foods.yml中的mole一项被触发（玩家吃掉地鼠）时执行
 */
function mole(event) {
    var player = event.getPlayer();
    player.setVelocity(player.getVelocity().add(new Vector(0.0, 1.68, 0.0)));
}