import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

public class Main {
    static int N;
    static int[][] dist;
    static List<int[]>[] list;
    static PriorityQueue<Integer>[] ans;
    static StringBuilder sb;
    public static void main(String[] args) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        StringTokenizer st=new StringTokenizer(br.readLine());

        N=Integer.parseInt(st.nextToken());
        int M=Integer.parseInt(st.nextToken());
        int K=Integer.parseInt(st.nextToken());

        st=new StringTokenizer(br.readLine());
        int s=Integer.parseInt(st.nextToken());
        int e=Integer.parseInt(st.nextToken());
        list=new ArrayList[N+1];
        ans=new PriorityQueue[N+1];
        for(int i=1;i<=N;i++){
            list[i]=new ArrayList<>();
            ans[i]=new PriorityQueue<>();

        }
        for(int i=0;i<M;i++){
            st=new StringTokenizer(br.readLine());
            int a=Integer.parseInt(st.nextToken());
            int b=Integer.parseInt(st.nextToken());
            int c=Integer.parseInt(st.nextToken());
            list[a].add(new int[]{b,c});
            list[b].add(new int[]{a,c});
        }
        sb=new StringBuilder();
            dij(s,e);
            int tax=0;
            int ret=Integer.MAX_VALUE;
        for(int i=1;i<=N;i++){
            ret=Math.min(ret,dist[e][i]);
        }
        sb.append(ret).append("\n");
        for(int i=0;i<K;i++){
            tax+=Integer.parseInt(br.readLine());

            ret=Integer.MAX_VALUE;
            for(int k=1;k<=N;k++){
                if(dist[e][k]+tax*k>0)
                ret = Math.min(ret, dist[e][k] + tax * k);

            }
            sb.append(ret).append("\n");
        }
        System.out.println(sb);
    }
    public static void dij(int s,int e){
        PriorityQueue<int[]> pq=new PriorityQueue<>(new Comparator<int[]>(){
            @Override
            public int compare(int[] a,int[] b){
                if(a[1]!=b[1]) return a[1]-b[1];
                else if(a[2]!=b[2]) return a[2]-b[2];
                else return a[0]-b[0];
            }
        });
        pq.offer(new int[]{s,0,0});
        dist=new int[N+1][N+1];
        for(int i=1;i<=N;i++) {
            Arrays.fill(dist[i],Integer.MAX_VALUE);
        }
        dist[s][0]=0;
        int[] dist2=new int[N+1];
        Arrays.fill(dist2,Integer.MAX_VALUE);
        dist2[s]=0;
        while(!pq.isEmpty()){
            int[] now=pq.poll();
            int cost=now[1];
            int roadquantity=now[2];

            if(dist[now[0]][roadquantity]<cost)continue;
            if(roadquantity>=N||now[0]==e) continue;

            for(int[] next:list[now[0]]){
                if(dist[next[0]][roadquantity+1]>cost+next[1]){
                    if(next[0]!=0&&dist2[next[0]]<=cost+next[1]) continue;

                    dist2[next[0]]=cost+next[1];
                    dist[next[0]][roadquantity+1]=cost+next[1];

                    pq.offer(new int[]{next[0],dist[next[0]][roadquantity+1],roadquantity+1});
                }

            }


        }

    }
}
