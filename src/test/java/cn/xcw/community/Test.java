package cn.xcw.community;

import java.util.*;

/**
 * @class: Test
 * @author: 邢成伟
 * @description: TODO
 **/

public class Test {

    public static int solution(int t,int[] nums){
        //每3个不同的角色就可以组成一个小组,第一个数字表示可供选择的角色数量T
        //先统计nums中0的数量
        int count=0;
        int min=Integer.MAX_VALUE;
        for(int i:nums){
            if(i!=0)
                count++;
            min=Math.min(min,i);
        }
        if(count<3)
            return 0;
        else
            return min;
    }

    public static void main(String[] args){

        int i = Test.solution(4, new int[]{2, 2, 3});
        System.out.println(i);
        PriorityQueue pq=new PriorityQueue();
        Queue<Integer> queue=new LinkedList<>();
        List l=new ArrayList();

    }
}
