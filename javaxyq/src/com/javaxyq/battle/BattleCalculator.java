/*
 * JavaXYQ Source Code
 * by kylixs
 * at 2009-11-14
 * http://javaxyq.googlecode.com
 * kylixs@qq.com
 */
package com.javaxyq.battle;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.javaxyq.model.MedicineItem;
import com.javaxyq.model.PlayerVO;
import com.javaxyq.util.ClassUtil;
import com.javaxyq.widget.Player;

/**
 * 战斗指令计算器
 * @author dewitt
 * @date 2009-11-14 create
 */
public class BattleCalculator {
	
	private Random random = new Random();
	private Map<Player,PlayerVO> datas = new HashMap<Player, PlayerVO>(); 
	private Map defends = new HashMap() ;
	private List ownsideTeam;
	private List adversaryTeam;
	public List calc(List<Command> cmdlist,List ownsideTeam,List adversaryTeam,Map params) {
		List results = new ArrayList();
		defends.clear();
		datas.clear();
		this.ownsideTeam = ownsideTeam;
		this.adversaryTeam = adversaryTeam;
		for (int i = 0; i < ownsideTeam.size(); i++) {
			Player player = (Player) ownsideTeam.get(i);
			PlayerVO vo = new PlayerVO(player.getData());
			datas.put(player, vo);
		}
		for (int i = 0; i < adversaryTeam.size(); i++) {
			Player player = (Player) adversaryTeam.get(i);
			PlayerVO vo = new PlayerVO(player.getData());
			datas.put(player, vo);
		}
		
		//按人物速度排序
		Collections.sort(cmdlist);
		//cmdlist = cmdlist.sort{cmd -> cmd.source.data.速度 + cmd.source.data.tmp速度}.reverse()
		System.out.println("cmdlist: $cmdlist");
		for (int i = 0; i < cmdlist.size(); i++) {
			Command cmd = (Command) cmdlist.get(i);
			Object result = this.invokeMethod(cmd.getCmd(),cmd);
			if(result!=null)results.add(result);
			if(ownsideTeam.size()==0 || adversaryTeam.size() == 0) {
				//如果一方的成员全部离开战场，则战斗结束
				break;
			}
		}
		return results;
	}
	
	private Command attack(Command cmd) {
		PlayerVO src = datas.get(cmd.getSource());
		PlayerVO target = datas.get(cmd.getTarget());
		if(target == null) {
			System.out.println("${cmd.target.name}已经退出战场，攻击无效。");
			return null;
		}
		if(src.hp <=0) {
			System.out.println("${src.name} 已倒下，无法发动攻击");
			return null;
		}else if(target.hp <= 0) {
			System.out.println("${target.name}已倒下，${src.name}的攻击无效");
			return null;
		}
		int hitpoints = 0;
		//判断是否命中
		float d = src.命中 - target.躲避;
		if(d<0)d = 0;
		boolean hit = (random.nextFloat()<= 0.7+d*1.0/src.命中);
		cmd.add("hit",hit);
		//计算伤害值
		if(hit) {
			d = src.伤害 - target.防御;
			if(d<0)d=0;
			hitpoints = (int) (0.05 * src.伤害 + (0.9+ (random.nextInt(20)-9)*1.0/100)*d);
			if(defends.get(target)!=null) {
				hitpoints *= 0.5;
				cmd.add("defend",true);
			}
			cmd.add("hitpoints",hitpoints);
			target.hp -= hitpoints;
			if(target.hp <0)target.hp = 0;
			cmd.add("die",true);
		}
		return cmd;
	}
	
	private Command magic(Command cmd) {
		PlayerVO src = datas.get(cmd.getSource());
		PlayerVO target = datas.get(cmd.getTarget());
		int usedmp = cmd.getInt("mp");
		if(target == null) {
			System.out.println("${cmd.target.name}已经退出战场，攻击无效。");
			return null;
		}
		if(src.hp <=0) {
			System.out.println("${src.name} 已倒下，无法发动攻击");
			return null;
		}else if(target.hp <= 0) {
			System.out.println("${target.name}已倒下，${src.name}的攻击无效");
			return null;
		}else if(src.mp < usedmp) {
			System.out.println("${src.name} 法力不足，施放法术失败！");
			return null;
		}
		src.mp -= usedmp;
		int hitpoints = 0;
		//判断是否命中
		float d = src.灵力 - target.灵力;
		if(d<0)d = 0;
		boolean hit = (random.nextFloat()<= 0.9+d*1.0/src.灵力);
		cmd.add("hit",hit);
		//TODO 计算法术伤害值
		if(hit) {
			//法术基本伤害系数
			int basehit = cmd.getInt("basehit");
			hitpoints =(int) (0.5*src.灵力*basehit+ 0.5*(1+ (random.nextInt(20)-9)*1.0/100)*d* basehit);
			cmd.add("hitpoints",hitpoints);
			target.hp -= hitpoints;
			if(target.hp <0)target.hp = 0;
			cmd.add("die",true);
		}
		return cmd;
	}

	private Command defend(Command cmd) {
		defends.put(datas.get(cmd.getSource()),true);
		return cmd;
	}
	
	private Command runaway(Command cmd) {
		boolean success = random.nextFloat()<0.9; 
		cmd.add("runaway",success);
		if(success) {
			datas.remove(cmd.getSource());
			ownsideTeam.remove(cmd.getSource());
			adversaryTeam.remove(cmd.getSource());
		}
		return cmd;
	}

	private Command item(Command cmd) {
		PlayerVO src = datas.get(cmd.getSource());
		PlayerVO target = datas.get(cmd.getTarget());
		if(target == null) {
			System.out.println("${cmd.target.name}已经退出战场，操作无效。");
			return null;
		}
		if(src.hp <=0) {
			System.out.println("${src.name} 已倒下，无法使用道具");
			return null;
		}else if(target.hp <= 0) {
			System.out.println("${target.name}已倒下，无法接受药物");
			return null;
		}		
		MedicineItem item = (MedicineItem) cmd.get("item");
		if(item != null) {
			Map efficacy = item.getEfficacyParams();
			Integer hpval = (Integer) efficacy.get("hp");
			if(hpval!=null) {
				target.hp += hpval;
				cmd.add("hp",hpval);
			}
			Integer mpval = (Integer) efficacy.get("mp");
			if(mpval!=null) {
				target.mp += mpval;
				cmd.add("mp",mpval);
			}
		}
		return cmd;
	}
	/**
	 * invoke a  method
	 * @param mName method name
	 * @param arg argument 
	 * @return
	 */
	private Object invokeMethod(String mName, Object arg) {
		try {
			Method m = this.getClass().getMethod(mName, arg.getClass());
			return m.invoke(this, arg);
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
		return null;
	}
	
}
