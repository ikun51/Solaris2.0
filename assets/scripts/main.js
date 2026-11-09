const SolarisJava = {
	get: function(name){
		return new Packages.rhino.NativeJavaClass(Vars.mods.getScripts().scope, Vars.mods.mainLoader().loadClass(name));
	}
};
SolarisJava.get("solaris2.content.SolarisItems").load();
SolarisJava.get("solaris2.content.SolarisLiquids").load();
SolarisJava.get("solaris2.content.SolarisStatuses").load();
SolarisJava.get("solaris2.content.SolarisWeathers").load();
SolarisJava.get("solaris2.content.SolarisUnits").load();
require("特殊/物品索引")
require("缩放强化");
require("魔改/PWR");
//炮台
require("建筑/炮台/铳炮")
require("建筑/炮台/重型铳炮")
require("建筑/炮台/多管发射器")
//配方
require("建筑/多配方/化学反应室")
require("建筑/多配方/化工厂")
require("建筑/多配方/电弧炉")
require("建筑/多配方/大型高炉")
require("建筑/多配方/高炉")
require("建筑/多配方/氮化炉")
require("建筑/多配方/核离心机")
require("建筑/多配方/酸液反应釜")
require("建筑/多配方/衰变室")
require("建筑/多配方/石化裂解炉")
require("建筑/多配方/重整炉")
require("建筑/多配方/大型火药工坊")
// 测试
require("test/powder-turret")
require("test/powder-mode-test")
